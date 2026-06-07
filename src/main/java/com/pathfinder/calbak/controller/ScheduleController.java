package com.pathfinder.calbak.controller;

import com.pathfinder.calbak.dto.ScheduleRecords.CreateRequest;
import com.pathfinder.calbak.dto.ScheduleRecords.ParsedResponse;
import com.pathfinder.calbak.dto.ScheduleRecords.ScheduleResponse;
import com.pathfinder.calbak.service.GeminiParserService;
import com.pathfinder.calbak.service.ScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final GeminiParserService geminiParserService;

    // 1. Gemini AI 일정 파싱 (다중 일정을 위해 List 반환으로 변경)
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ParsedResponse>> parseSchedule(
        @RequestParam(required = false) String text,
        @RequestPart(required = false) List<MultipartFile> images) {

        // 텍스트도 없고 이미지도 없으면 에러
        if ((text == null || text.isBlank()) && (images == null || images.isEmpty())) {
            throw new IllegalArgumentException("텍스트나 이미지 중 하나는 반드시 입력해야 합니다.");
        }

        // 이미지 리스트가 있다면 각 파일이 유효한지 검증
        if (images != null && !images.isEmpty()) {
            boolean hasValidFile = images.stream()
                .anyMatch(file -> file != null && !file.isEmpty());
            if (!hasValidFile) {
                throw new IllegalArgumentException("유효한 이미지 파일이 없습니다.");
            }
        }

        List<ParsedResponse> response = geminiParserService.parseSchedule(text, images);
        return ResponseEntity.ok(response);
    }

    // 2. 일정 최종 생성
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
        Authentication authentication,
        @Valid @RequestBody CreateRequest request) {
        String email = authentication.getName();
        ScheduleResponse response = scheduleService.createSchedule(email, request);
        return ResponseEntity.ok(response);
    }

    // 3. 일간 타임테이블 조회
    @GetMapping("/daily")
    public ResponseEntity<List<ScheduleResponse>> getDailyTimetable(
        Authentication authentication,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String email = authentication.getName();
        List<ScheduleResponse> response = scheduleService.getDailyTimetable(email, date);
        return ResponseEntity.ok(response);
    }

    // 4. 월간 달력 조회 (month 검증 조건 추가)
    @GetMapping("/monthly")
    public ResponseEntity<List<ScheduleResponse>> getMonthlyCalendar(
        Authentication authentication,
        @RequestParam @Min(2000) int year,
        @RequestParam @Min(1) @Max(12) int month) {
        String email = authentication.getName();
        List<ScheduleResponse> response = scheduleService.getMonthlyCalendar(email, year, month);
        return ResponseEntity.ok(response);
    }
}
