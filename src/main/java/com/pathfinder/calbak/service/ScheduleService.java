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
import com.pathfinder.calbak.repository.TeamRepository;
import com.pathfinder.calbak.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CategoryRepository categoryRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    // 일정 생성
    @Transactional
    public ScheduleResponse createSchedule(String email, CreateRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        Team team = null;
        if (request.teamId() != null) {
            team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀입니다."));
        }

        Schedule schedule = Schedule.builder()
            .user(user)
            .category(category)
            .team(team)
            .title(request.title())
            .content(request.content())
            .location(request.location())
            .startDate(request.startDate())
            .startTime(request.startTime())
            .endDate(request.endDate())
            .endTime(request.endTime())
            .isAllDay(request.isAllDay())
            .repeatPattern(request.repeatPattern())
            .repeatEndDate(request.repeatEndDate())
            .reminderMinutes(request.reminderMinutes())
            .status(ScheduleStatus.ACTIVE)
            .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        return ScheduleResponse.from(savedSchedule);
    }

    // 일간 타임테이블 조회
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
