package com.pathfinder.calbak.service;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.domain.entity.Schedule;
import com.pathfinder.calbak.domain.entity.Team;
import com.pathfinder.calbak.domain.entity.User;
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
import java.util.List;
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
    private final TeamMemberRepository teamMemberRepository; // 피드백 반영용 추가

    // 일정 생성
    @Transactional
    public ScheduleResponse createSchedule(String email, CreateRequest request) {
        // 날짜/시간 정합성 검증
        validateScheduleDateTime(request.startDate(), request.startTime(), request.endDate(), request.endTime(),
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

        Schedule schedule = Schedule.builder()
            .user(user)
            .category(category)
            .team(team)
            .title(request.title())
            .content(request.content())
            .location(request.location())
            .startDate(request.startDate())
            .startTime(Boolean.TRUE.equals(request.isAllDay()) ? null : request.startTime())
            .endDate(request.endDate())
            .endTime(Boolean.TRUE.equals(request.isAllDay()) ? null : request.endTime())
            .isAllDay(request.isAllDay())
            .repeatPattern(request.repeatPattern())
            .repeatEndDate(request.repeatEndDate())
            .reminderMinutes(request.reminderMinutes())
            .status(ScheduleStatus.ACTIVE)
            .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        return ScheduleResponse.from(savedSchedule);
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

    public List<ScheduleResponse> getDailyTimetable(String email, LocalDate date) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return scheduleRepository
            .findSchedulesWithinDateRange(user.getId(), date, date, ScheduleStatus.ACTIVE)
            .stream()
            .map(ScheduleResponse::from)
            .collect(Collectors.toList());
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
