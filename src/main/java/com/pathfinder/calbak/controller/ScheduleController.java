package com.pathfinder.calbak.controller;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.dto.ScheduleRecords.CreateRequest;
import com.pathfinder.calbak.dto.ScheduleRecords.ParsedResponse;
import com.pathfinder.calbak.dto.ScheduleRecords.ScheduleResponse;
import com.pathfinder.calbak.dto.ScheduleRecords.UpdateRequest;
import com.pathfinder.calbak.service.CategoryService;
import com.pathfinder.calbak.service.GeminiParserService;
import com.pathfinder.calbak.service.ScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final GeminiParserService geminiParserService;
    private final CategoryService categoryService;

    // Gemini AI 일정 파싱 - 다중 일정을 위해 List 반환
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ParsedResponse>> parseSchedule(
        @RequestParam(required = false) String text,
        @RequestPart(required = false) List<MultipartFile> images) {

        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) {
                    throw new IllegalArgumentException("빈 이미지 파일은 허용되지 않습니다.");
                }
            }
        }

        // 텍스트도 없고 이미지도 없으면 에러
        if ((text == null || text.isBlank()) && (images == null || images.isEmpty())) {
            throw new IllegalArgumentException("텍스트나 이미지 중 하나는 반드시 입력해야 합니다.");
        }

        List<ParsedResponse> response = geminiParserService.parseSchedule(text, images);
        return ResponseEntity.ok(response);
    }

    // 일정 생성
    @PostMapping
    public ResponseEntity<List<ScheduleResponse>> createSchedule(
        Authentication authentication,
        @Valid @RequestBody CreateRequest request) {
        String email = authentication.getName();
        List<ScheduleResponse> response = scheduleService.createSchedule(email, request);
        return ResponseEntity.ok(response);
    }

    // 텍스트/이미지 파싱 + 자동 일정 등록 통합 API
    // 모든 일정이 하나의 트랜잭션 안에서 저장되므로 중간 실패 시 전체 롤백 보장
    @PostMapping(value = "/parse-and-create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ScheduleResponse>> parseAndCreate(
        Authentication authentication,
        @RequestParam(required = false) String text,
        @RequestPart(required = false) List<MultipartFile> images) {

        String email = authentication.getName();

        // /parse 엔드포인트와 동일한 빈 입력 검증
        if ((text == null || text.isBlank()) && (images == null || images.isEmpty())) {
            throw new IllegalArgumentException("텍스트나 이미지 중 하나는 반드시 입력해야 합니다.");
        }

        // 1. 파싱
        List<ParsedResponse> parsedList = geminiParserService.parseSchedule(text, images);

        // 2. 일단 유저의 첫 번째 카테고리 조회
        Category firstCategory = categoryService.getFirstCategory(email);

        // 3. 파싱 결과를 원자적으로 일괄 저장 (서비스 레이어의 단일 트랜잭션으로 처리)
        List<ScheduleResponse> results =
            scheduleService.createSchedulesBatch(email, firstCategory.getId(), parsedList);

        return ResponseEntity.ok(results);
    }

    // 일반 일정 수정 API
    @PatchMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
        Authentication authentication,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateRequest request) {
        String email = authentication.getName();
        ScheduleResponse response = scheduleService.updateSchedule(id, email, request);
        return ResponseEntity.ok(response);
    }

    // AI 이미지/텍스트 기반 자동 수정 API
    // parse-and-create와 동일하게 List<MultipartFile> images
    @PatchMapping(value = "/{id}/parse-and-update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ScheduleResponse> parseAndUpdateSchedule(
        Authentication authentication,
        @PathVariable UUID id,
        @RequestParam(required = false) String text,
        @RequestPart(required = false) List<MultipartFile> images) {

        String email = authentication.getName();

        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) {
                    throw new IllegalArgumentException("빈 이미지 파일은 허용되지 않습니다.");
                }
            }
        }

        if ((text == null || text.isBlank()) && (images == null || images.isEmpty())) {
            throw new IllegalArgumentException("수정할 텍스트나 이미지를 입력해주세요.");
        }

        ScheduleResponse response = scheduleService.parseAndUpdateSchedule(id, email, text, images);
        return ResponseEntity.ok(response);
    }

    // 일정 완료
    @PatchMapping("/{id}/complete")
    public ResponseEntity<String> completeSchedule(
        Authentication authentication,
        @PathVariable UUID id) {
        String email = authentication.getName();
        scheduleService.completeSchedule(id, email);
        return ResponseEntity.ok("일정이 완료되었습니다.");
    }

    // 일정 삭제
    @PatchMapping("/{id}/delete")
    public ResponseEntity<String> deleteSchedule(
        Authentication authentication,
        @PathVariable UUID id) {
        String email = authentication.getName();
        scheduleService.deleteSchedule(id, email);
        return ResponseEntity.ok("일정이 삭제되었습니다.");
    }

    // 일간 타임테이블 조회
    @GetMapping("/daily")
    public ResponseEntity<List<ScheduleResponse>> getDailyTimetable(
        Authentication authentication,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String email = authentication.getName();
        List<ScheduleResponse> response = scheduleService.getDailyTimetable(email, date);
        return ResponseEntity.ok(response);
    }

    // 24시간 타임테이블 내보내기 API (방학계획표 UI용)
    @GetMapping("/daily/export")
    public ResponseEntity<Map<Integer, List<ScheduleResponse>>> exportDailyTimetable(
        Authentication authentication,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String email = authentication.getName();
        Map<Integer, List<ScheduleResponse>> response = scheduleService.exportDailyTimetable(email, date);
        return ResponseEntity.ok(response);
    }

    // 월간 달력 조회
    @GetMapping("/monthly")
    public ResponseEntity<List<ScheduleResponse>> getMonthlyCalendar(
        Authentication authentication,
        @RequestParam @Min(2000) int year,
        @RequestParam @Min(1) @Max(12) int month) {
        String email = authentication.getName();
        List<ScheduleResponse> response = scheduleService.getMonthlyCalendar(email, year, month);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body("잘못된 요청입니다: " + e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }

    // 예상치 못한 서버 에러는 로그로만 기록하고 내부 정보는 클라이언트에 노출하지 않음
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("서버 처리 중 예상치 못한 오류 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("서버 처리 중 오류가 발생했습니다.");
    }
}
