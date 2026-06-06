package com.pathfinder.calbak.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.domain.enums.Enums;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest // DB 관련 빈만 빠르게 로드
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("유저를 저장하고 닉네임 중복 검사가 정상 작동하는지 확인한다")
    void existsByNicknameTest() {
        // given: 테스트용 유저 생성
        User user = User.builder()
            .email("test@google.com")
            .provider(Enums.Provider.GOOGLE)
            .name("테스터")
            .nickname("캘박마스터")
            .systemRole(Enums.SystemRole.USER)
            .status(Enums.UserStatus.ACTIVE)
            .build();

        userRepository.save(user);

        // when: 저장된 닉네임과 없는 닉네임으로 각각 조회
        boolean isExist = userRepository.existsByNickname("캘박마스터");
        boolean isNotExist = userRepository.existsByNickname("없는닉네임");

        // then: 결과 검증
        assertThat(isExist).isTrue();
        assertThat(isNotExist).isFalse();
    }
}
