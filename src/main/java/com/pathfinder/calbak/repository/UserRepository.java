package com.pathfinder.calbak.repository;

import com.pathfinder.calbak.domain.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    // 이메일로 유저 찾기 (로그인 시 사용)
    Optional<User> findByEmail(String email);

    // 닉네임 중복 검사용
    boolean existsByNickname(String nickname);

    // JWT 필터에서 UUID만 필요할 때 사용하는 경량 프로젝션 (User 전체 엔티티 로딩 방지)
    @Query("SELECT u.id FROM User u WHERE u.email = :email")
    Optional<UUID> findIdByEmail(@Param("email") String email);
}
