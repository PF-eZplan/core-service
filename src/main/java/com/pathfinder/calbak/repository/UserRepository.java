package com.pathfinder.calbak.repository;

import com.pathfinder.calbak.domain.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    // 이메일로 유저 찾기 (로그인 시 사용)
    Optional<User> findByEmail(String email);

    // 닉네임 중복 검사용
    boolean existsByNickname(String nickname);
}
