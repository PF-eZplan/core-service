package com.pathfinder.calbak.service;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.domain.entity.Schedule;
import com.pathfinder.calbak.domain.entity.Team;
import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.domain.enums.Enums.RepeatPattern;
import com.pathfinder.calbak.domain.enums.Enums.ScheduleStatus;
import com.pathfinder.calbak.dto.ScheduleRecords.CreateRequest;
import com.pathfinder.calbak.dto.ScheduleRecords.ParsedResponse;
import com.pathfinder.calbak.dto.ScheduleRecords.ScheduleResponse;
import com.pathfinder.calbak.dto.ScheduleRecords.UpdateRequest;
import com.pathfinder.calbak.repository.CategoryRepository;
import com.pathfinder.calbak.repository.ScheduleRepository;
import com.pathfinder.calbak.repository.TeamMemberRepository;
import com.pathfinder.calbak.repository.TeamRepository;
import com.pathfinder.calbak.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CategoryRepository categoryRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final GeminiParserService geminiParserService; // AI 수정 기능을 위해 주입 추가

    // 일정 생성
    @Transactional
    public List<ScheduleResponse> createSchedule(String email, CreateRequest request) {

        // 시작 및 종료 시간 보정 (종일 일정이 아닌데 종료 시간이 없다면 시작 시간 + 1시간)
        LocalTime effectiveStartTime = request.startTime();
        LocalTime effectiveEndTime = request.endTime();

        if (Boolean.FALSE.equals(request.isAllDay())) {
            if (effectiveStartTime == null) {
                effectiveStartTime = LocalTime.of(0, 0); // 시작 시간마저 없다면 기본 00:00 할당
            }
            if (effectiveEndTime == null) {
                effectiveEndTime = effectiveStartTime.plusHours(1);
            }
        }

        // 날짜/시간 정합성 검증 (보정된 시간 사용)
        validateScheduleDateTime(request.startDate(), effectiveStartTime, request.endDate(), effectiveEndTime,
            request.isAllDay());

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        Team team = null;
        if (request.teamId() != null) {
            team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀입니다."));

            // 팀 권한 검증 추가
            if (!teamMemberRepository.existsByTeamIdAndUserId(team.getId(), user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "팀 소속이 아닙니다.");
            }
        }

        UUID groupId = UUID.randomUUID(); // 연관된 반복 일정을 묶기 위한 그룹 ID
        List<Schedule> schedulesToSave = new ArrayList<>();

        LocalDate currentDate = request.startDate();

        // 반복 종료일이 시작일보다 빠른 경우 명시적 에러 처리
        LocalDate repeatEnd;
        if (request.repeatEndDate() != null) {
            if (request.repeatEndDate().isBefore(request.startDate())) {
                throw new IllegalArgumentException(
                    "반복 종료일(" + request.repeatEndDate() + ")은 시작일(" + request.startDate() + ")보다 빠를 수 없습니다.");
            }
            repeatEnd = request.repeatEndDate();
        } else if (request.repeatPattern() != null && request.repeatPattern() != RepeatPattern.NONE) {
            // 반복 종료일이 없으면 기본 1년 뒤로 설정
            // null이면 repeatEnd = startDate가 되어 while 루프가 1회만 실행, 단건으로만 저장되는 버그 방지
            repeatEnd = request.startDate().plusYears(1);
        } else {
            // 단건 일정: while 루프 1회만 돌도록 시작일과 동일하게 맞춤
            repeatEnd = request.startDate();
        }

        long daysBetween = ChronoUnit.DAYS.between(request.startDate(), request.endDate());

        if (request.repeatPattern() == null || request.repeatPattern() == RepeatPattern.NONE) {
            schedulesToSave.add(
                buildSchedule(user, category, team, groupId, currentDate, request.endDate(), effectiveStartTime,
                    effectiveEndTime, request));
        } else {
            // repeatEnd가 시작일과 동일하면 이 while문은 딱 1번만 돌고 끝남 (단건 생성)
            while (!currentDate.isAfter(repeatEnd)) {
                LocalDate currentEnd = currentDate.plusDays(daysBetween);
                schedulesToSave.add(
                    buildSchedule(user, category, team, groupId, currentDate, currentEnd, effectiveStartTime,
                        effectiveEndTime, request));

                switch (request.repeatPattern()) {
                    case DAILY -> currentDate = currentDate.plusDays(1);
                    case WEEKLY -> currentDate = currentDate.plusWeeks(1);
                    case MONTHLY -> currentDate = currentDate.plusMonths(1);
                    case YEARLY -> currentDate = currentDate.plusYears(1);
                    default -> currentDate = currentDate.plusDays(1);
                }
            }
        }

        List<Schedule> savedSchedules = scheduleRepository.saveAll(schedulesToSave);
        return savedSchedules.stream().map(ScheduleResponse::from).collect(Collectors.toList());
    }

    // Gemini 파싱 결과를 단일 트랜잭션 안에서 원자적으로 일괄 저장
    // parse-and-create 엔드포인트에서 호출 (부분 커밋 방지)
    @Transactional
    public List<ScheduleResponse> createSchedulesBatch(String email, UUID categoryId,
                                                       List<ParsedResponse> parsedList) {
        List<ScheduleResponse> results = new ArrayList<>();
        for (ParsedResponse parsed : parsedList) {
            CreateRequest createRequest = new CreateRequest(
                categoryId,
                null,                    // teamId: 일단 파싱 결과에는 팀 정보 없음
                parsed.title(),
                parsed.content(),
                parsed.location(),
                parsed.startDate(),
                parsed.startTime(),
                parsed.endDate(),
                parsed.endTime(),
                parsed.isAllDay(),
                parsed.repeatPattern(),  // Gemini가 파싱한 반복 패턴 그대로 사용
                parsed.repeatEndDate(),  // Gemini가 파싱한 반복 종료일 그대로 사용
                30                       // 기본 알림 시간 30분
            );
            results.addAll(createSchedule(email, createRequest));
        }
        return results;
    }

    // 일반 수동 폼 일정 수정
    @Transactional
    public ScheduleResponse updateSchedule(UUID scheduleId, String email, UpdateRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        if (schedule.getTeam() == null && !schedule.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 일정만 수정 가능합니다.");
        }
        if (schedule.getTeam() != null && !teamMemberRepository.existsByTeamIdAndUserId(schedule.getTeam().getId(),
            user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "팀 일정을 수정할 권한이 없습니다.");
        }

        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

        // 여기서 request는 UpdateRequest 레코드 객체이므로 메서드명 그대로 접근
        LocalTime effectiveStartTime = request.startTime();
        LocalTime effectiveEndTime = request.endTime();

        if (Boolean.FALSE.equals(request.isAllDay())) {
            if (effectiveStartTime == null) {
                effectiveStartTime = LocalTime.of(0, 0);
            }
            if (effectiveEndTime == null) {
                effectiveEndTime = effectiveStartTime.plusHours(1);
            }
        }

        validateScheduleDateTime(request.startDate(), effectiveStartTime, request.endDate(), effectiveEndTime,
            request.isAllDay());

        schedule.update(category, request.title(), request.content(), request.location(),
            request.startDate(), effectiveStartTime, request.endDate(), effectiveEndTime,
            request.isAllDay(), request.repeatPattern(), request.repeatEndDate(), request.reminderMinutes());

        return ScheduleResponse.from(schedule);
    }

    // AI 이미지/텍스트 기반 단건 자동 수정
    // 다중 이미지 리스트 수용 및 다중 일정 반환 시 빠른 예외 처리(Fail Fast)
    @Transactional
    public ScheduleResponse parseAndUpdateSchedule(UUID scheduleId, String email, String text,
                                                   List<MultipartFile> images) {
        List<ParsedResponse> parsedList = geminiParserService.parseSchedule(text, images);

        if (parsedList.isEmpty()) {
            throw new IllegalArgumentException("분석된 일정이 없습니다.");
        }
        if (parsedList.size() > 1) {
            throw new IllegalArgumentException("여러 일정이 감지되었습니다. 하나의 일정만 입력하세요.");
        }

        ParsedResponse parsed = parsedList.get(0);

        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        UpdateRequest updateReq = new UpdateRequest(
            schedule.getCategory().getId(), parsed.title(), parsed.content(), parsed.location(),
            parsed.startDate(), parsed.startTime(), parsed.endDate(), parsed.endTime(),
            parsed.isAllDay(), parsed.repeatPattern(), parsed.repeatEndDate(), schedule.getReminderMinutes()
        );

        return updateSchedule(scheduleId, email, updateReq);
    }

    // 보정된 시간을 받도록 파라미터 추가
    private Schedule buildSchedule(User user, Category category, Team team, UUID groupId,
                                   LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime,
                                   CreateRequest request) {
        return Schedule.builder()
            .user(user)
            .category(category)
            .team(team)
            .groupId(groupId)
            .title(request.title())
            .content(request.content())
            .location(request.location())
            .startDate(startDate)
            .startTime(Boolean.TRUE.equals(request.isAllDay()) ? null : startTime)
            .endDate(endDate)
            .endTime(Boolean.TRUE.equals(request.isAllDay()) ? null : endTime)
            .isAllDay(request.isAllDay())
            .repeatPattern(request.repeatPattern())
            .repeatEndDate(request.repeatEndDate())
            .reminderMinutes(request.reminderMinutes())
            .status(ScheduleStatus.ACTIVE)
            .build();
    }

    // 시간 논리 정합성 밸리데이션 헬퍼
    private void validateScheduleDateTime(LocalDate startDate, LocalTime startTime, LocalDate endDate,
                                          LocalTime endTime, Boolean isAllDay) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료 날짜는 시작 날짜보다 빠를 수 없습니다.");
        }
        if (Boolean.FALSE.equals(isAllDay) && endDate.isEqual(startDate) && startTime != null && endTime != null
            && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 빠를 수 없습니다.");
        }
        if (Boolean.FALSE.equals(isAllDay) && (startTime == null || endTime == null)) {
            throw new IllegalArgumentException("종일 일정이 아닌 경우 시작 및 종료 시간이 필수입니다.");
        }
    }

    // 일정 상태(완료, 삭제 등) 변경
    // Proxy 지연 로딩 에러 방지를 위해 User 엔티티를 명시적으로 불러와서 비교
    @Transactional
    public void updateScheduleStatus(UUID scheduleId, ScheduleStatus status, String email) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        if (!schedule.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 일정 상태만 변경할 수 있습니다.");
        }

        schedule.updateStatus(status);
        // JPA Flush 에러(Read-Only 충돌) 막기 위해 명시적으로 saveAndFlush 호출
        scheduleRepository.saveAndFlush(schedule);
    }

    // 명시적 완료 로직
    @Transactional
    public void completeSchedule(UUID scheduleId, String email) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!schedule.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 일정만 수정 가능합니다.");
        }

        schedule.updateStatus(ScheduleStatus.COMPLETED);
        scheduleRepository.saveAndFlush(schedule); // 명시적 저장 및 플러시 강제
    }

    // 명시적 삭제 로직
    @Transactional
    public void deleteSchedule(UUID scheduleId, String email) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!schedule.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 일정만 수정 가능합니다.");
        }

        schedule.updateStatus(ScheduleStatus.DELETED);
        scheduleRepository.saveAndFlush(schedule); // 명시적 저장 및 플러시 강제
    }

    public List<ScheduleResponse> getDailyTimetable(String email, LocalDate date) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // ACTIVE와 COMPLETED 상태 모두 넘겨주어 조회
        return scheduleRepository
            .findSchedulesWithinDateRange(
                user.getId(),
                date,
                date,
                List.of(ScheduleStatus.ACTIVE, ScheduleStatus.COMPLETED)
            )
            .stream()
            .map(ScheduleResponse::from)
            .collect(Collectors.toList());
    }

    // 24시간 표 구조(0시~23시)로 하루 데이터를 맵핑해서 내보내기 (방학계획표st 용도)
    public Map<Integer, List<ScheduleResponse>> exportDailyTimetable(String email, LocalDate date) {
        List<ScheduleResponse> dailySchedules = getDailyTimetable(email, date);
        Map<Integer, List<ScheduleResponse>> timeTableMap = new HashMap<>();

        for (int i = 0; i < 24; i++) {
            timeTableMap.put(i, new ArrayList<>());
        }

        for (ScheduleResponse schedule : dailySchedules) {
            int startHour;
            int endHour;

            if (Boolean.TRUE.equals(schedule.isAllDay())) {
                // 종일 일정: 0시~23시 전체
                startHour = 0;
                endHour = 23;
            } else if (date.equals(schedule.startDate()) && date.equals(schedule.endDate())) {
                // 단일 일정 (시작일 = 종료일): 실제 시작/종료 시간 그대로 사용
                startHour = schedule.startTime() == null ? 0 : schedule.startTime().getHour();
                endHour = schedule.endTime() == null ? 23 : schedule.endTime().getHour();
            } else if (date.equals(schedule.startDate())) {
                // 다중 일정의 첫째 날: 실제 시작 시간 ~ 23시
                startHour = schedule.startTime() == null ? 0 : schedule.startTime().getHour();
                endHour = 23;
            } else if (date.equals(schedule.endDate())) {
                // 다중 일정의 마지막 날: 0시 ~ 실제 종료 시간
                startHour = 0;
                endHour = schedule.endTime() == null ? 23 : schedule.endTime().getHour();
            } else {
                // 다중 일정의 중간 날: 0시~23시 전체
                startHour = 0;
                endHour = 23;
            }

            // 자정을 넘기는 일정 처리 (예: 23시 시작 → 1시 종료)
            if (startHour <= endHour) {
                // 일반 케이스: 같은 날 안에 끝나는 일정
                for (int h = startHour; h <= endHour; h++) {
                    timeTableMap.get(h).add(schedule);
                }
            } else {
                // 자정을 넘기는 일정: startHour..23 구간과 0..endHour 구간으로 분리
                for (int h = startHour; h < 24; h++) {
                    timeTableMap.get(h).add(schedule);
                }
                for (int h = 0; h <= endHour; h++) {
                    timeTableMap.get(h).add(schedule);
                }
            }
        }

        return timeTableMap;
    }

    // 월간 달력 조회
    public List<ScheduleResponse> getMonthlyCalendar(String email, int year, int month) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // ACTIVE와 COMPLETED 상태 모두 넘겨주어 조회
        return scheduleRepository
            .findSchedulesWithinDateRange(
                user.getId(),
                startDate,
                endDate,
                List.of(ScheduleStatus.ACTIVE, ScheduleStatus.COMPLETED)
            )
            .stream()
            .map(ScheduleResponse::from)
            .collect(Collectors.toList());
    }
}
