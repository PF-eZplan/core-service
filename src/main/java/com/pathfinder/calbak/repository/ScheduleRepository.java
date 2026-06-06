package com.pathfinder.calbak.repository;

import com.pathfinder.calbak.domain.entity.Schedule;
import com.pathfinder.calbak.domain.enums.Enums.ScheduleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    // "나의 개인 일정" + "내가 속한 팀들의 일정"을 한 번에 조회하는 커스텀 쿼리
    @Query("SELECT s FROM Schedule s " +
        "LEFT JOIN FETCH s.category " +
        "LEFT JOIN FETCH s.team " +
        "WHERE s.user.id = :userId " +
        "OR s.team.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.user.id = :userId)")
    List<Schedule> findMyAndTeamSchedules(@Param("userId") UUID userId);

    // 특정 날짜 범위 내의 개인 및 팀 일정 조회 (타임테이블 및 월간 달력용)
    // status를 문자열 리터럴 대신 파라미터로 받아 enum 타입 안전성 보장
    @Query("SELECT s FROM Schedule s " +
        "LEFT JOIN FETCH s.category " +
        "LEFT JOIN FETCH s.team " +
        "WHERE (s.user.id = :userId " +
        "OR s.team.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.user.id = :userId)) " +
        "AND s.status = :status " +
        "AND s.startDate <= :endDate " +
        "AND s.endDate >= :startDate " +
        "ORDER BY s.startDate ASC, s.startTime ASC NULLS LAST")
    List<Schedule> findSchedulesWithinDateRange(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("status") ScheduleStatus status
    );
}
