package com.pathfinder.calbak.domain.entity;

import com.pathfinder.calbak.domain.enums.Enums.RepeatPattern;
import com.pathfinder.calbak.domain.enums.Enums.ScheduleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 일정 생성자 (팀 일정이어도 최초 등록자는 존재함)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category; // 일정이 속한 카테고리 연결

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team; // null이면 개인 일정, 값이 있으면 해당 모임에 공유되는 일정

    // 반복/다중 일정을 하나로 묶기 위한 그룹 ID
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    private RepeatPattern repeatPattern; // 반복 종류

    private LocalDate repeatEndDate; // 반복이 종료되는 날짜

    @Column(nullable = false)
    private String title; // 일정 제목

    private String content; // 추가 메모 및 세부 내용

    private String location; // 장소 정보

    @Column(nullable = false)
    private LocalDate startDate; // 시작 날짜

    private LocalTime startTime; // 시작 시간 (종일 일정일 경우 null 가능)

    @Column(nullable = false)
    private LocalDate endDate; // 단일 일정이면 startDate와 동일

    private LocalTime endTime; // 종료 시간

    @Column(nullable = false)
    private Boolean isAllDay; // 종일 일정 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status; // 정상, 완료, 삭제 상태

    private Integer reminderMinutes; // 15분 전, 30분 전 등 리마인드 여부

    public void updateStatus(ScheduleStatus status) {
        this.status = status;
    }

    public void update(Category category, String title, String content, String location,
                       LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime,
                       Boolean isAllDay, RepeatPattern repeatPattern, LocalDate repeatEndDate,
                       Integer reminderMinutes) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.location = location;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.isAllDay = isAllDay;
        this.repeatPattern = repeatPattern;
        this.repeatEndDate = repeatEndDate;
        this.reminderMinutes = reminderMinutes;
    }
}
