package com.pathfinder.calbak.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.domain.enums.Enums;
import com.pathfinder.calbak.dto.UserAdditionalInfoRequest;
import com.pathfinder.calbak.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("닉네임이 이미 존재하면 IllegalArgumentException을 던진다")
    void updateAdditionalInfo_DuplicateNickname() {
        // given
        User user = User.builder().email("test@test.com").nickname("기존유저").build();
        UserAdditionalInfoRequest request = new UserAdditionalInfoRequest(
            "test@test.com", "중복닉네임", Enums.Gender.MALE, Enums.AgeGroup.AGE_20S,
            null, null, null, null, Enums.NotificationSetting.NONE
        );

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateAdditionalInfo(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 사용 중인 닉네임입니다.");
    }
}
