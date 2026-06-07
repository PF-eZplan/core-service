package com.pathfinder.calbak.service;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.domain.entity.Schedule;
import com.pathfinder.calbak.domain.entity.Team;
import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.domain.enums.Enums.RepeatPattern;
import com.pathfinder.calbak.domain.enums.Enums.ScheduleStatus;
import com.pathfinder.calbak.dto.ScheduleRecords.CreateRequest;
import com.pathfinder.calbak.dto.ScheduleRecords.ScheduleResponse;
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
        // 종료일이 안 적혀있으면 시작일과 동일하게 맞춤 -> 무한루프 없이 단 1건만 생성 후 종료됨!
        LocalDate repeatEnd = request.repeatEndDate() != null ? request.repeatEndDate() : request.startDate();
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

        return scheduleRepository
            .findSchedulesWithinDateRange(user.getId(), date, date, ScheduleStatus.ACTIVE)
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
            int startHour = (schedule.isAllDay() || schedule.startTime() == null) ? 0 : schedule.startTime().getHour();
            int endHour = (schedule.isAllDay() || schedule.endTime() == null) ? 23 : schedule.endTime().getHour();

            for (int h = startHour; h <= endHour; h++) {
                timeTableMap.get(h).add(schedule);
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

        return scheduleRepository
            .findSchedulesWithinDateRange(user.getId(), startDate, endDate, ScheduleStatus.ACTIVE)
            .stream()
            .map(ScheduleResponse::from)
            .collect(Collectors.toList());
    }
}
