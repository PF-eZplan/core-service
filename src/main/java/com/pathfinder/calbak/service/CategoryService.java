package com.pathfinder.calbak.service;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.dto.CategoryRecords.CategoryResponse;
import com.pathfinder.calbak.repository.CategoryRepository;
import com.pathfinder.calbak.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, String email, String name, String colorCode) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다. (Email: " + email + ")"));

        // 2. 카테고리 존재 여부 확인
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. (ID: " + categoryId + ")"));

        // 3. 본인 소유 확인
        if (category.getUser() == null || !category.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 카테고리만 수정 가능합니다.");
        }

        // 4. 이름 중복 검사 (자기 자신 제외)
        if (categoryRepository.existsByUserIdAndNameAndIdNot(user.getId(), name, categoryId)) {
            throw new IllegalArgumentException("이미 해당 이름의 카테고리가 존재합니다: " + name);
        }

        // 5. 업데이트
        // flush() 제거: @Transactional 커밋 시점에 Hibernate 더티 체킹이 자동으로 UPDATE 실행
        // flush()를 명시적으로 호출하면 AuditingEntityListener -> AuditorAware -> findByEmail() -> Hibernate가 쿼리 전 다시 flush() -> 무한 재귀 -> StackOverflowError 발생
        category.update(name, colorCode);

        return CategoryResponse.from(category);
    }

    public Category getFirstCategory(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return categoryRepository.findFirstByUserOrderByCreatedAtAsc(user)
            .orElseThrow(() -> new IllegalArgumentException("사용자의 카테고리를 찾을 수 없습니다."));
    }
}
