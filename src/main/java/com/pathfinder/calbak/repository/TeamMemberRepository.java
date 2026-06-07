package com.pathfinder.calbak.repository;

import com.pathfinder.calbak.domain.entity.TeamMember;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);
}
