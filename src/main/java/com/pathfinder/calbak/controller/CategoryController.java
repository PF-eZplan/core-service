package com.pathfinder.calbak.controller;

import com.pathfinder.calbak.dto.CategoryRecords.CategoryResponse;
import com.pathfinder.calbak.dto.CategoryRecords.UpdateCategoryRequest;
import com.pathfinder.calbak.service.CategoryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
        Authentication authentication,
        @PathVariable UUID categoryId,
        @Valid @RequestBody UpdateCategoryRequest request) {

        CategoryResponse response = categoryService.updateCategory(
            categoryId,
            authentication.getName(),
            request.name(),
            request.colorCode()
        );
        return ResponseEntity.ok(response);
    }

    // 중복 이름 에러를 500이 아닌 400 에러로 잡아주는 핸들러
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body("잘못된 요청입니다: " + e.getMessage());
    }

    // ResponseStatusException(403 등)이 Exception 핸들러에 삼켜져 400으로 반환되는 문제 방지 -> 원래 HTTP 상태코드를 그대로 유지해서 반환
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }

    // 예상치 못한 서버 에러는 500으로 반환하고 내부 메시지는 노출하지 않음
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("서버 처리 중 오류가 발생했습니다.");
    }
}
