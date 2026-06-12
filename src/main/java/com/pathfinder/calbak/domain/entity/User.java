package com.pathfinder.calbak.domain.entity;

import com.pathfinder.calbak.domain.enums.Enums.AgeGroup;
import com.pathfinder.calbak.domain.enums.Enums.Gender;
import com.pathfinder.calbak.domain.enums.Enums.NotificationStatus;
import com.pathfinder.calbak.domain.enums.Enums.Provider;
import com.pathfinder.calbak.domain.enums.Enums.SystemRole;
import com.pathfinder.calbak.domain.enums.Enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email; // 구글 및 일반 로그인 식별자

    private String password; // 일반 가입자용 암호화 비밀번호 (구글 가입자는 null)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider; // 가입 출처 (LOCAL, GOOGLE 등)

    @Column(nullable = false)
    private String name; // 실명

    @Column(unique = true)
    private String nickname; // 앱 내 닉네임 (가입 직후엔 null)

    @Enumerated(EnumType.STRING)
    private Gender gender; // 성별 (Enum)

    @Enumerated(EnumType.STRING)
    private AgeGroup ageGroup; // 나이대 (Enum)

    private String job; // 직업 (복수 선택 시 콤마로 이어붙여 저장)
    private String usagePurpose; // 용도 (복수 선택 시 콤마로 이어붙여 저장)

    private LocalTime sleepTime; // 수면 시작 시간
    private LocalTime wakeUpTime; // 기상 시간

    @Enumerated(EnumType.STRING)
    private NotificationStatus notificationStatus; // 알림 설정 (Enum)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SystemRole systemRole; // 전체 시스템 접근 권한 (USER, ADMIN)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status; // 유저 상태 (정상, 정지, 탈퇴)

    // 온보딩 일괄 처리 메서드
    public void completeOnboarding(String nickname, Gender gender, AgeGroup ageGroup,
                                   String job, String usagePurpose,
                                   LocalTime sleepTime, LocalTime wakeUpTime,
                                   NotificationStatus notificationStatus) {
        this.nickname = nickname;
        this.gender = gender;
        this.ageGroup = ageGroup;
        this.job = job;
        this.usagePurpose = usagePurpose;
        this.sleepTime = sleepTime;
        this.wakeUpTime = wakeUpTime;
        this.notificationStatus = notificationStatus;
    }
}
