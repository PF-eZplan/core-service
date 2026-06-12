package com.pathfinder.calbak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.domain.enums.Enums;
import com.pathfinder.calbak.domain.enums.Enums.NotificationStatus;
import com.pathfinder.calbak.dto.UserAdditionalInfoRequest;
import com.pathfinder.calbak.dto.UserNicknameUpdateRequest;
import com.pathfinder.calbak.exception.DuplicateNicknameException;
import com.pathfinder.calbak.exception.UserNotFoundException;
import com.pathfinder.calbak.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "test@test.com",
                null
            )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("닉네임이 이미 존재하면 DuplicateNicknameException을 던진다")
    void updateAdditionalInfo_DuplicateNickname() {
        // given
        User user = User.builder().email("test@test.com").nickname("기존유저").build();
        UserAdditionalInfoRequest request = new UserAdditionalInfoRequest(
            "중복닉네임", Enums.Gender.MALE, Enums.AgeGroup.AGE_20S,
            null, null, null, null, NotificationStatus.NO
        );

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateAdditionalInfo(request))
            .isInstanceOf(DuplicateNicknameException.class)
            .hasMessage("이미 사용 중인 닉네임입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 유저면 UserNotFoundException을 던진다")
    void updateAdditionalInfo_UserNotFound() {

        // given
        UserAdditionalInfoRequest request = new UserAdditionalInfoRequest(
            "닉네임",
            Enums.Gender.MALE,
            Enums.AgeGroup.AGE_20S,
            null,
            null,
            null,
            null,
            NotificationStatus.NO
        );

        given(userRepository.findByEmail("test@test.com"))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateAdditionalInfo(request))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessage("존재하지 않는 유저입니다.");
    }

    @Test
    @DisplayName("정상적인 추가 정보 입력 시 유저 정보가 수정된다")
    void updateAdditionalInfo_Success() {

        User user = User.builder()
            .email("test@test.com")
            .nickname("기존닉네임")
            .build();

        UserAdditionalInfoRequest request =
            new UserAdditionalInfoRequest(
                "새닉네임",
                Enums.Gender.MALE,
                Enums.AgeGroup.AGE_20S,
                "대학생",
                "수업관리",
                null,
                null,
                NotificationStatus.NO
            );

        given(userRepository.findByEmail("test@test.com"))
            .willReturn(Optional.of(user));

        given(userRepository.existsByNickname("새닉네임"))
            .willReturn(false);

        userService.updateAdditionalInfo(request);

        assertThat(user.getNickname()).isEqualTo("새닉네임");
        assertThat(user.getGender()).isEqualTo(Enums.Gender.MALE);
        assertThat(user.getAgeGroup()).isEqualTo(Enums.AgeGroup.AGE_20S);
        assertThat(user.getJob()).isEqualTo("대학생");
        assertThat(user.getUsagePurpose()).isEqualTo("수업관리");
        assertThat(user.getNotificationStatus()).isEqualTo(NotificationStatus.NO);

        verify(userRepository)
            .findByEmail("test@test.com");

        verify(userRepository)
            .existsByNickname("새닉네임");
    }

    @Test
    @DisplayName("기존 닉네임과 같으면 중복 검사를 수행하지 않는다")
    void updateAdditionalInfo_SameNickname() {

        User user = User.builder()
            .email("test@test.com")
            .nickname("기존닉네임")
            .build();

        UserAdditionalInfoRequest request =
            new UserAdditionalInfoRequest(
                "기존닉네임",
                Enums.Gender.MALE,
                Enums.AgeGroup.AGE_20S,
                null,
                null,
                null,
                null,
                NotificationStatus.NO
            );

        given(userRepository.findByEmail("test@test.com"))
            .willReturn(Optional.of(user));

        userService.updateAdditionalInfo(request);

        verify(userRepository)
            .findByEmail("test@test.com");

        verify(userRepository, never())
            .existsByNickname(anyString());
    }

    @Test
    @DisplayName("닉네임 단건 수정 시 닉네임이 정상적으로 변경된다")
    void updateNickname_Success() {
        // given
        User user = User.builder()
            .email("test@test.com")
            .nickname("기존닉네임")
            .build();
        UserNicknameUpdateRequest request = new UserNicknameUpdateRequest("새로운닉네임");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("새로운닉네임")).willReturn(false);

        // when
        userService.updateNickname(request);

        // then
        assertThat(user.getNickname()).isEqualTo("새로운닉네임");
        verify(userRepository).findByEmail("test@test.com");
        verify(userRepository).existsByNickname("새로운닉네임");
    }

    @Test
    @DisplayName("변경하려는 닉네임이 기존 닉네임과 동일하면 중복 검사 없이 바로 리턴한다")
    void updateNickname_SameNickname() {
        // given
        User user = User.builder()
            .email("test@test.com")
            .nickname("기존닉네임")
            .build();
        UserNicknameUpdateRequest request = new UserNicknameUpdateRequest("기존닉네임");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        // when
        userService.updateNickname(request);

        // then
        assertThat(user.getNickname()).isEqualTo("기존닉네임");
        verify(userRepository).findByEmail("test@test.com");
        // DB 쿼리를 아끼기 위해 existsByNickname이 절대 호출되지 않아야 함
        verify(userRepository, never()).existsByNickname(anyString());
    }

    @Test
    @DisplayName("변경하려는 닉네임이 이미 존재하면 DuplicateNicknameException을 던진다")
    void updateNickname_DuplicateNickname() {
        // given
        User user = User.builder()
            .email("test@test.com")
            .nickname("기존닉네임")
            .build();
        UserNicknameUpdateRequest request = new UserNicknameUpdateRequest("중복된닉네임");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("중복된닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(request))
            .isInstanceOf(DuplicateNicknameException.class)
            .hasMessage("이미 사용 중인 닉네임입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 유저가 닉네임 수정을 시도하면 UserNotFoundException을 던진다")
    void updateNickname_UserNotFound() {
        // given
        UserNicknameUpdateRequest request = new UserNicknameUpdateRequest("새로운닉네임");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(request))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessage("존재하지 않는 유저입니다.");
    }

    // 단건 수정 중 동시성 이슈 발생 검증
    // 가짜 에러 객체 생성 시 'nickname' 이라는 단어를 포함하도록 수정
    @Test
    @DisplayName("닉네임 단건 수정 시 동시성 이슈로 DB Unique 제약조건이 발생하면 DuplicateNicknameException을 던진다")
    void updateNickname_ConcurrentDuplicate() {
        User user = User.builder().email("test@test.com").nickname("기존닉네임").build();
        UserNicknameUpdateRequest request = new UserNicknameUpdateRequest("동시성닉네임");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("동시성닉네임")).willReturn(false); // pre-check는 통과 (동시 접근 상황)

        // flush 시점에 DB Unique Constraint 예외 발생 시뮬레이션
        doThrow(
            new DataIntegrityViolationException("duplicate key value violates unique constraint 'uk_user_nickname'"))
            .when(userRepository).flush();

        assertThatThrownBy(() -> userService.updateNickname(request))
            .isInstanceOf(DuplicateNicknameException.class)
            .hasMessage("이미 사용 중인 닉네임입니다.");
    }

    // 온보딩 중 동시성 이슈 발생 검증
    // 가짜 에러 객체 생성 시 'nickname' 이라는 단어를 포함하도록 수정
    @Test
    @DisplayName("온보딩 추가 정보 입력 시 동시성 이슈로 DB Unique 제약조건이 발생하면 DuplicateNicknameException을 던진다")
    void updateAdditionalInfo_ConcurrentDuplicate() {
        User user = User.builder().email("test@test.com").nickname("기존닉네임").build();
        UserAdditionalInfoRequest request = new UserAdditionalInfoRequest(
            "동시성닉네임", Enums.Gender.MALE, Enums.AgeGroup.AGE_20S,
            null, null, null, null, NotificationStatus.NO
        );

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("동시성닉네임")).willReturn(false); // pre-check 통과

        // flush 시점에 예외 발생 시뮬레이션
        doThrow(
            new DataIntegrityViolationException("duplicate key value violates unique constraint 'uk_user_nickname'"))
            .when(userRepository).flush();

        assertThatThrownBy(() -> userService.updateAdditionalInfo(request))
            .isInstanceOf(DuplicateNicknameException.class)
            .hasMessage("이미 사용 중인 닉네임입니다.");
    }
}
