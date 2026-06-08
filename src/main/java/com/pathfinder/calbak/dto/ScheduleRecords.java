package com.pathfinder.calbak.dto;

import com.pathfinder.calbak.domain.entity.Schedule;
import com.pathfinder.calbak.domain.enums.Enums.RepeatPattern;
import com.pathfinder.calbak.domain.enums.Enums.ScheduleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class ScheduleRecords {

    // 1. Gemini 파싱 요청 DTO
    public record ParseRequest(
        @NotBlank(message = "분석할 텍스트를 입력해주세요.")
        String text
    ) {
    }

    // 2. Gemini 파싱 결과 (JSON 변환용)
    public record ParsedResponse(
        String title,
        String content,
        String location,
        LocalDate startDate,
        LocalTime startTime,
        LocalDate endDate,
        LocalTime endTime,
        Boolean isAllDay,
        RepeatPattern repeatPattern, // AI가 인식한 반복 패턴
        LocalDate repeatEndDate // AI가 인식한 종료일
    ) {
    }

    // 3. 일정 생성/수정 요청 DTO
    public record CreateRequest(
        @NotNull(message = "카테고리 ID는 필수입니다.") UUID categoryId,
        UUID teamId,
        @NotBlank(message = "일정 제목은 필수입니다.") String title,
        String content,
        String location,
        @NotNull(message = "시작 날짜는 필수입니다.") LocalDate startDate,
        LocalTime startTime,
        @NotNull(message = "종료 날짜는 필수입니다.") LocalDate endDate,
        LocalTime endTime,
        @NotNull(message = "종일 여부는 필수입니다.") Boolean isAllDay,
        RepeatPattern repeatPattern,
        LocalDate repeatEndDate,
        Integer reminderMinutes
    ) {
    }

    // 4. 일정 응답 DTO (타임테이블 및 캘린더 조회 시 사용)
    public record ScheduleResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        String categoryColor,
        UUID teamId,
        String teamName,
        String title,
        String content,
        String location,
        LocalDate startDate,
        LocalTime startTime,
        LocalDate endDate,
        LocalTime endTime,
        Boolean isAllDay,
        RepeatPattern repeatPattern,
        LocalDate repeatEndDate,
        ScheduleStatus status,
        UUID groupId // 응답 시 그룹 ID 반환
    ) {
        // 엔티티를 DTO로 변환
        public static ScheduleResponse from(Schedule schedule) {
            return new ScheduleResponse(
                schedule.getId(),
                schedule.getCategory().getId(),
                schedule.getCategory().getName(),
                schedule.getCategory().getColorCode(),
                schedule.getTeam() != null ? schedule.getTeam().getId() : null,
                schedule.getTeam() != null ? schedule.getTeam().getName() : null,
                schedule.getTitle(),
                schedule.getContent(),
                schedule.getLocation(),
                schedule.getStartDate(),
                schedule.getStartTime(),
                schedule.getEndDate(),
                schedule.getEndTime(),
                schedule.getIsAllDay(),
                schedule.getRepeatPattern(),
                schedule.getRepeatEndDate(),
                schedule.getStatus(),
                schedule.getGroupId()
            );
        }
    }
}
