package com.pathfinder.calbak.repository;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.domain.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    // 본인 소유의 다른 카테고리 중 동일한 이름이 있는지 확인 (현재 수정 중인 id 제외)
    boolean existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID id);
    
    // 유저의 카테고리 중 생성일이 가장 빠른 것을 첫 번째로 조회
    Optional<Category> findFirstByUserOrderByCreatedAtAsc(User user);
}
