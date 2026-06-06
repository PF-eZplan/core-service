package com.pathfinder.calbak.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.domain.enums.Enums;
import com.pathfinder.calbak.dto.UserAdditionalInfoRequest;
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
            null, null, null, null, Enums.NotificationSetting.NONE
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
            Enums.NotificationSetting.NONE
        );

        given(userRepository.findByEmail("test@test.com"))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateAdditionalInfo(request))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessage("존재하지 않는 유저입니다.");
    }
}
