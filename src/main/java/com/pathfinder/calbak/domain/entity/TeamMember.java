package com.pathfinder.calbak.domain.entity;

import com.pathfinder.calbak.domain.enums.Enums.TeamRole;
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
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팀과 유저 사이의 다대다 관계를 풀어주기 위한 중간 매핑 엔티티
 */
@Entity
@Table(
    name = "team_members",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"team_id", "user_id"})}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TeamMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team; // 속한 모임

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 속한 유저

    @Enumerated(EnumType.STRING)
    @Column(name = "team_role", nullable = false)
    private TeamRole teamRole; // 모임 내 권한 (LEADER, MEMBER)
}
