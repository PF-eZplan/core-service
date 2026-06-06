package com.pathfinder.calbak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.domain.enums.Enums;
import com.pathfinder.calbak.repository.CategoryRepository;
import com.pathfinder.calbak.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private CustomOAuth2UserService oAuth2UserService;

    @Test
    @DisplayName("신규 유저 로그인 시 유저가 저장되고 기본 카테고리 5개가 생성된다")
    void shouldSaveUserAndCreateDefaultCategoriesWhenNewUser() {
        // given
        given(oAuth2User.getAttribute("email")).willReturn("new@google.com");
        given(oAuth2User.getAttribute("name")).willReturn("신규유저");
        given(userRepository.findByEmail("new@google.com")).willReturn(Optional.empty());

        User savedUser = User.builder()
            .email("new@google.com")
            .name("신규유저")
            .provider(Enums.Provider.GOOGLE)
            .systemRole(Enums.SystemRole.USER)
            .status(Enums.UserStatus.ACTIVE)
            .build();
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Category>> categoryCaptor = ArgumentCaptor.forClass(List.class);

        // when
        oAuth2UserService.processUser(oAuth2User);

        // then
        verify(userRepository, times(1)).save(any(User.class));
        verify(categoryRepository, times(1)).saveAll(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue()).hasSize(5);
    }

    @Test
    @DisplayName("기존 유저 로그인 시 유저 저장과 카테고리 생성이 일어나지 않는다")
    void shouldNotSaveUserOrCategoriesWhenExistingUser() {
        // given
        given(oAuth2User.getAttribute("email")).willReturn("existing@google.com");
        given(oAuth2User.getAttribute("name")).willReturn("기존유저");

        User existingUser = User.builder()
            .email("existing@google.com")
            .name("기존유저")
            .provider(Enums.Provider.GOOGLE)
            .systemRole(Enums.SystemRole.USER)
            .status(Enums.UserStatus.ACTIVE)
            .build();
        given(userRepository.findByEmail("existing@google.com"))
            .willReturn(Optional.of(existingUser));

        // when
        oAuth2UserService.processUser(oAuth2User);

        // then
        verify(userRepository, never()).save(any(User.class));
        verify(categoryRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("신규 유저 저장 시 이메일과 구글 provider가 올바르게 설정된다")
    void shouldSetCorrectEmailAndProviderForNewUser() {
        // given
        given(oAuth2User.getAttribute("email")).willReturn("new@google.com");
        given(oAuth2User.getAttribute("name")).willReturn("신규유저");
        given(userRepository.findByEmail("new@google.com")).willReturn(Optional.empty());

        User savedUser = User.builder()
            .email("new@google.com")
            .name("신규유저")
            .provider(Enums.Provider.GOOGLE)
            .systemRole(Enums.SystemRole.USER)
            .status(Enums.UserStatus.ACTIVE)
            .build();
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // when
        oAuth2UserService.processUser(oAuth2User);

        // then
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getEmail()).isEqualTo("new@google.com");
        assertThat(capturedUser.getProvider()).isEqualTo(Enums.Provider.GOOGLE);
        assertThat(capturedUser.getSystemRole()).isEqualTo(Enums.SystemRole.USER);
        assertThat(capturedUser.getStatus()).isEqualTo(Enums.UserStatus.ACTIVE);
    }
}
