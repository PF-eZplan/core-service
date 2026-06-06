package com.pathfinder.calbak.service;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.domain.enums.Enums;
import com.pathfinder.calbak.repository.CategoryRepository;
import com.pathfinder.calbak.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest); // 구글 서버 호출 (테스트 불가 영역)
        processUser(oAuth2User); // 테스트 가능한 영역으로 분리
        return oAuth2User;
    }

    // 테스트 가능하도록 분리된 메서드
    @Transactional
    public OAuth2User processUser(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("missing_email"), "구글 계정에서 이메일을 가져올 수 없습니다."
            );
        }

        // DB에 없는 새로운 유저라면 회원가입(Save) 및 카테고리 자동 생성
        userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                .email(email)
                .name(name)
                .provider(Enums.Provider.GOOGLE)
                .systemRole(Enums.SystemRole.USER)
                .status(Enums.UserStatus.ACTIVE)
                .build();
            User savedUser = userRepository.save(newUser);

            // 최초 가입 시 기본 카테고리 5개 자동 INSERT
            createDefaultCategories(savedUser);
            return savedUser;
        });

        return oAuth2User; // 시큐리티 세션에 저장될 객체 반환
    }

    private void createDefaultCategories(User user) {
        List<Category> defaultCategories = List.of(
            Category.builder().user(user).name("개인").colorCode("#FF5733").build(),
            Category.builder().user(user).name("업무").colorCode("#33FF57").build(),
            Category.builder().user(user).name("학교").colorCode("#FFC300").build(),
            Category.builder().user(user).name("약속").colorCode("#3357FF").build(),
            Category.builder().user(user).name("건강").colorCode("#FF33A8").build()
        );
        categoryRepository.saveAll(defaultCategories);
    }
}
