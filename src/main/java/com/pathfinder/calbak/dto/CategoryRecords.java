package com.pathfinder.calbak.dto;

import com.pathfinder.calbak.domain.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public class CategoryRecords {

    public record UpdateCategoryRequest(
        @NotBlank(message = "카테고리 이름은 필수입니다.")
        String name,

        @NotBlank(message = "색상 코드는 필수입니다.")
        // #으로 시작하는 3자리 또는 6자리 헥스 코드만 허용 (예: #FFF, #FF5733)
        @Pattern(
            regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
            message = "색상 코드는 #으로 시작하는 3자리 또는 6자리 헥스 코드여야 합니다. (예: #FFF, #FF5733)"
        )
        String colorCode
    ) {
    }

    public record CategoryResponse(
        UUID id,
        String name,
        String colorCode
    ) {
        public static CategoryResponse from(Category category) {
            return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getColorCode()
            );
        }
    }
}
