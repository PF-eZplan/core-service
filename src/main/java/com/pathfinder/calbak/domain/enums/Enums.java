package com.pathfinder.calbak.domain.enums;

public class Enums {
    // 시스템 권한 (일반 유저 vs 관리자)
    public enum SystemRole {
        USER, ADMIN
    }

    // 유저 상태 (정상, 정지, 탈퇴)
    public enum UserStatus {
        ACTIVE, SUSPENDED, WITHDRAWN
    }

    // 모임 내 역할 (팀장, 팀원)
    public enum TeamRole {
        LEADER, MEMBER
    }

    // 일정 상태 (활성, 완료, 삭제됨)
    public enum ScheduleStatus {
        ACTIVE, COMPLETED, DELETED
    }

    // 반복 패턴 (없음, 매일, 매주, 매월, 매년)
    public enum RepeatPattern {
        NONE, DAILY, WEEKLY, MONTHLY, YEARLY
    }

    // 가입 출처 (자체, 구글, 애플)
    public enum Provider {
        LOCAL, GOOGLE, APPLE
    }

    // 성별
    public enum Gender {
        MALE, FEMALE
    }

    // 나이대
    public enum AgeGroup {
        AGE_10S, AGE_20S, AGE_30S, AGE_40S_PLUS
    }

    // 알림 설정 (10분 전, 30분 전, 1시간 전, 없음)
    public enum NotificationSetting {
        MIN_10, MIN_30, HOUR_1, NONE
    }
}
