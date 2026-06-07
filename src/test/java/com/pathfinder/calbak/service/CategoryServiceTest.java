package com.pathfinder.calbak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.dto.CategoryRecords.CategoryResponse;
import com.pathfinder.calbak.repository.CategoryRepository;
import com.pathfinder.calbak.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("기존 카테고리의 이름과 색상 수정에 성공한다")
    void updateCategory_Success() {
        String email = "test@test.com";
        User user = User.builder().id(UUID.randomUUID()).email(email).build();
        UUID categoryId = UUID.randomUUID();

        Category category = Category.builder()
            .id(categoryId).user(user).name("기본분류").colorCode("#FFFFFF").build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByUserIdAndNameAndIdNot(user.getId(), "동아리", categoryId)).willReturn(false);

        CategoryResponse response = categoryService.updateCategory(categoryId, email, "동아리", "#FF5733");

        assertThat(response.name()).isEqualTo("동아리");
        assertThat(response.colorCode()).isEqualTo("#FF5733");
    }

    @Test
    @DisplayName("다른 사람의 카테고리를 수정하려고 하면 권한 예외가 발생한다")
    void updateCategory_Forbidden() {
        String email = "hacker@test.com";
        User hacker = User.builder().id(UUID.randomUUID()).email(email).build();
        User owner = User.builder().id(UUID.randomUUID()).email("owner@test.com").build();
        UUID categoryId = UUID.randomUUID();

        Category category = Category.builder().id(categoryId).user(owner).name("기본분류").build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(hacker));
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, email, "해킹시도", "#000"))
            .isInstanceOf(ResponseStatusException.class)
            // ResponseStatusException의 전체 메시지는 "403 FORBIDDEN "본인의 카테고리만 수정 가능합니다.""
            // hasMessageContaining으로 메시지 일부만 검증
            .hasMessageContaining("본인의 카테고리만 수정 가능합니다.");
    }

    @Test
    @DisplayName("다른 카테고리와 이름이 중복되게 변경하려고 하면 예외가 발생한다")
    void updateCategory_DuplicateName_ThrowsException() {
        String email = "test@test.com";
        User user = User.builder().id(UUID.randomUUID()).email(email).build();
        UUID categoryId = UUID.randomUUID();

        Category category = Category.builder().id(categoryId).user(user).name("개인").build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        // 본인의 다른 카테고리 중 "학교"라는 이름이 이미 있다고 가정
        given(categoryRepository.existsByUserIdAndNameAndIdNot(user.getId(), "학교", categoryId)).willReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, email, "학교", "#FF5733"))
            .isInstanceOf(IllegalArgumentException.class)
            // 실제 서비스가 던지는 메시지: "이미 해당 이름의 카테고리가 존재합니다: 학교"
            // hasMessageContaining으로 카테고리명 포함 여부와 무관하게 핵심 문구만 검증
            .hasMessageContaining("이미 해당 이름의 카테고리가 존재합니다");
    }
}
