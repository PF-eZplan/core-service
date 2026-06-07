package com.pathfinder.calbak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.domain.entity.Schedule;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    @DisplayName("정상적인 요청 시 일정이 생성된다")
    void createSchedule_Success() {
        String email = "test@test.com";
        User user = User.builder().id(UUID.randomUUID()).email(email).build();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("학교").colorCode("#FFF").build();

        CreateRequest request = new CreateRequest(
            categoryId, null, "수강신청", "빠르게", "집",
            LocalDate.now(), null, LocalDate.now(), null, true,
            RepeatPattern.NONE, null, 10
        );

        Schedule savedSchedule = Schedule.builder()
            .id(UUID.randomUUID())
            .user(user)
            .category(category)
            .title("수강신청")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .isAllDay(true)
            .status(ScheduleStatus.ACTIVE)
            .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(scheduleRepository.save(any(Schedule.class))).willReturn(savedSchedule);

        ScheduleResponse response = scheduleService.createSchedule(email, request);

        assertThat(response.title()).isEqualTo("수강신청");
        assertThat(response.categoryName()).isEqualTo("학교");
        assertThat(response.isAllDay()).isTrue();
    }

    @Test
    @DisplayName("날짜 정합성이 어긋나면 예외를 발생시킨다 (종료 날짜가 시작 날짜보다 빠름)")
    void createSchedule_Fail_Validation() {
        String email = "test@test.com";
        CreateRequest request = new CreateRequest(
            UUID.randomUUID(), null, "오류일정", null, null,
            LocalDate.of(2026, 6, 12), LocalTime.of(10, 0),
            LocalDate.of(2026, 6, 10), LocalTime.of(12, 0), false,
            RepeatPattern.NONE, null, null
        );

        assertThatThrownBy(() -> scheduleService.createSchedule(email, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("종료 날짜는 시작 날짜보다 빠를 수 없습니다.");
    }
}
