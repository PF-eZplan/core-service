package com.pathfinder.calbak.repository;

import com.pathfinder.calbak.domain.entity.Schedule;
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
}
