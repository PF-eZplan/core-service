package com.pathfinder.calbak.controller;

import com.pathfinder.calbak.domain.entity.Category;
import com.pathfinder.calbak.dto.ScheduleRecords.CreateRequest;
import com.pathfinder.calbak.dto.ScheduleRecords.ParsedResponse;
import com.pathfinder.calbak.dto.ScheduleRecords.ScheduleResponse;
import com.pathfinder.calbak.service.CategoryService;
import com.pathfinder.calbak.service.GeminiParserService;
import com.pathfinder.calbak.service.ScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
    @PostMapping("/parse-and-create")
    public ResponseEntity<List<ScheduleResponse>> parseAndCreate(
        Authentication authentication,
        @RequestParam(required = false) String text,
        @RequestPart(required = false) List<MultipartFile> images) {

        String email = authentication.getName();

        // 1. 파싱
        List<ParsedResponse> parsedList = geminiParserService.parseSchedule(text, images);

        // 2. 일단 유저의 첫 번째 카테고리 자동 조회 (서비스 레이어에서 가져온 객체 사용)
        Category firstCategory = categoryService.getFirstCategory(email);

        List<ScheduleResponse> results = new ArrayList<>();

        // 3. 파싱 결과를 CreateRequest로 변환 및 등록
        for (ParsedResponse parsed : parsedList) {
            CreateRequest createRequest = new CreateRequest(
                firstCategory.getId(),
                null, // teamId
                parsed.title(),
                parsed.content(),
                parsed.location(),
                parsed.startDate(),
                parsed.startTime(),
                parsed.endDate(),
                parsed.endTime(),
                parsed.isAllDay(),
                parsed.repeatPattern(),
                parsed.repeatEndDate(),
                30 // 기본 알림 시간
            );
            results.addAll(scheduleService.createSchedule(email, createRequest));
        }

        return ResponseEntity.ok(results);
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

    // Enum 변환 에러 및 JPA Transaction 에러 원인 출력
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause != rootCause.getCause()) {
            rootCause = rootCause.getCause();
        }
        return ResponseEntity.badRequest()
            .body("서버 처리 중 에러가 발생했습니다: " + e.getMessage() + "\n[상세 원인]: " + rootCause.getMessage());
    }
}
