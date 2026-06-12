package com.pathfinder.calbak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.domain.entity.Schedule;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

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
    @Mock
    private GeminiParserService geminiParserService;

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
        given(scheduleRepository.saveAll(anyList())).willReturn(List.of(savedSchedule));

        List<ScheduleResponse> response = scheduleService.createSchedule(email, request);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("수강신청");
        assertThat(response.get(0).categoryName()).isEqualTo("학교");
        assertThat(response.get(0).isAllDay()).isTrue();
    }

    @Test
    @DisplayName("종일 일정이 아니고 종료 시간이 없으면 시작 시간 + 1시간으로 자동 설정된다")
    void createSchedule_AutoSetEndTime() {
        String email = "test@test.com";
        User user = User.builder().id(UUID.randomUUID()).email(email).build();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("회의").colorCode("#FFF").build();

        CreateRequest request = new CreateRequest(
            categoryId, null, "짧은 회의", null, null,
            LocalDate.now(), LocalTime.of(14, 0), LocalDate.now(), null, false, // 종료 시간 null
            RepeatPattern.NONE, null, 10
        );

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        // saveAll 호출 시 넘겨받은 리스트를 그대로 반환하도록 설정하여 서비스 내에서 보정된 값이 담겼는지 확인
        given(scheduleRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        List<ScheduleResponse> response = scheduleService.createSchedule(email, request);

        assertThat(response).hasSize(1);
        // null이었던 endTime이 startTime(14:00)의 1시간 뒤인 15:00으로 설정되었는지 검증
        assertThat(response.get(0).endTime()).isEqualTo(LocalTime.of(15, 0));
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

    @Test
    @DisplayName("반복 일정인데 종료일(repeatEndDate)이 없어도 에러를 뱉지 않고 단 1일치(시작일) 일정만 생성하고 무사히 종료된다")
    void createSchedule_RepeatWithNoEndDate_CreatesSingleSchedule() {
        String email = "test@test.com";
        User user = User.builder().id(UUID.randomUUID()).email(email).build();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("동아리").colorCode("#FFFFFF").build();

        CreateRequest request = new CreateRequest(
            categoryId, null, "동아리 활동", null, null,
            LocalDate.of(2026, 6, 10), null, LocalDate.of(2026, 6, 10), null, true,
            RepeatPattern.WEEKLY, null, null
        );

        Schedule savedSchedule = Schedule.builder()
            .title("동아리 활동")
            .category(category)
            .startDate(LocalDate.of(2026, 6, 10))
            .status(ScheduleStatus.ACTIVE)
            .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(scheduleRepository.saveAll(anyList())).willReturn(List.of(savedSchedule));

        List<ScheduleResponse> response = scheduleService.createSchedule(email, request);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("동아리 활동");
    }

    @Test
    @DisplayName("일정 단건 수정 시 정상적으로 반영된다")
    void updateSchedule_Success() {
        String email = "test@test.com";
        User user = User.builder().id(UUID.randomUUID()).email(email).build();
        Category category = Category.builder().id(UUID.randomUUID()).name("기존").colorCode("#000").build();
        Category newCategory = Category.builder().id(UUID.randomUUID()).name("새카테고리").colorCode("#FFF").build();
        Schedule schedule = Schedule.builder().id(UUID.randomUUID()).user(user).category(category).build();

        UpdateRequest request = new UpdateRequest(
            newCategory.getId(), "수정제목", "수정내용", "수정장소",
            LocalDate.now(), null, LocalDate.now(), null, true, RepeatPattern.NONE, null, 15
        );

        given(scheduleRepository.findById(schedule.getId())).willReturn(Optional.of(schedule));
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(categoryRepository.findById(request.categoryId())).willReturn(Optional.of(newCategory));

        ScheduleResponse response = scheduleService.updateSchedule(schedule.getId(), email, request);

        assertThat(response.title()).isEqualTo("수정제목");
        assertThat(response.categoryName()).isEqualTo("새카테고리");
        assertThat(schedule.getTitle()).isEqualTo("수정제목");
    }

    // 일정 엔티티 내부 검증(update) 시 예외 처리 테스트
    @Test
    @DisplayName("일정 단건 수정 시 필수 값이 누락되거나 정합성이 안 맞으면 예외가 발생한다")
    void updateSchedule_Fail_Validation() {
        String email = "test@test.com";
        User user = User.builder().id(UUID.randomUUID()).email(email).build();
        Category category = Category.builder().id(UUID.randomUUID()).name("기존").colorCode("#000").build();
        Schedule schedule = Schedule.builder().id(UUID.randomUUID()).user(user).category(category).build();

        // 비정상 데이터: 제목이 비어있음 (" ")
        UpdateRequest request = new UpdateRequest(
            category.getId(), "   ", null, null,
            LocalDate.now(), null, LocalDate.now(), null, true, RepeatPattern.NONE, null, 15
        );

        given(scheduleRepository.findById(schedule.getId())).willReturn(Optional.of(schedule));
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(categoryRepository.findById(request.categoryId())).willReturn(Optional.of(category));

        assertThatThrownBy(() -> scheduleService.updateSchedule(schedule.getId(), email, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("일정 제목은 비어있을 수 없습니다.");
    }

    @Test
    @DisplayName("AI를 이용해 일정을 정상적으로 파싱하고 수정한다")
    void parseAndUpdateSchedule_Success() {
        String email = "test@test.com";
        User user = User.builder().id(UUID.randomUUID()).email(email).build();
        Category category = Category.builder().id(UUID.randomUUID()).name("기존").colorCode("#000").build();
        Schedule schedule = Schedule.builder().id(UUID.randomUUID()).user(user).category(category).build();

        ParsedResponse parsedResponse = new ParsedResponse(
            "AI수정제목", null, null, LocalDate.now(), null, LocalDate.now(), null, true, RepeatPattern.NONE, null
        );

        given(geminiParserService.parseSchedule(anyString(), any())).willReturn(List.of(parsedResponse));
        given(scheduleRepository.findById(schedule.getId())).willReturn(Optional.of(schedule));
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(categoryRepository.findById(category.getId())).willReturn(Optional.of(category));

        // 파라미터가 List<MultipartFile> 타입으로 변경된 것 반영 (null 강제 형변환)
        ScheduleResponse response = scheduleService.parseAndUpdateSchedule(schedule.getId(), email, "AI 수정요청",
            (List<MultipartFile>) null);

        assertThat(response.title()).isEqualTo("AI수정제목");
        assertThat(schedule.getTitle()).isEqualTo("AI수정제목");
    }

    // AI 다중 파싱 방어 로직(Fail-fast) 테스트
    @Test
    @DisplayName("AI 수정 시 파싱 결과가 2개 이상이면 예외가 발생한다 (다중 일정 수정 방지)")
    void parseAndUpdateSchedule_Fail_MultipleResults() {
        String email = "test@test.com";
        UUID scheduleId = UUID.randomUUID();

        // 2개의 파싱 결과를 반환하도록 Mock 설정
        ParsedResponse parsed1 = new ParsedResponse("첫번째", null, null, LocalDate.now(), null, LocalDate.now(), null,
            true, RepeatPattern.NONE, null);
        ParsedResponse parsed2 = new ParsedResponse("두번째", null, null, LocalDate.now(), null, LocalDate.now(), null,
            true, RepeatPattern.NONE, null);

        given(geminiParserService.parseSchedule(anyString(), any())).willReturn(List.of(parsed1, parsed2));

        assertThatThrownBy(
            () -> scheduleService.parseAndUpdateSchedule(scheduleId, email, "AI 수정요청", (List<MultipartFile>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("여러 일정이 감지되었습니다. 하나의 일정만 입력하세요.");
    }
}
