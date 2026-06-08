package com.pathfinder.calbak.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 사용자가 일정을 분류하기 위해 생성하는 카테고리 엔티티
 */
@Entity
@Table(
    name = "categories",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "name"})}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 이 카테고리의 주인

    @Column(nullable = false)
    private String name; // 카테고리 이름 (학교, 개인, 업무 등)

    @Column(nullable = false)
    private String colorCode; // 헥스코드 (예: #FF5733)
    
    public void update(String name, String colorCode) {
        this.name = name;
        this.colorCode = colorCode;
    }
}
