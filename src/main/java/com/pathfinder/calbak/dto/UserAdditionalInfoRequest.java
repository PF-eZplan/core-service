package com.pathfinder.calbak.dto;

import com.pathfinder.calbak.domain.enums.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record UserAdditionalInfoRequest(
    @NotBlank(message = "이메일은 필수입니다.") String email, // 현재 토큰 필터가 없으므로 식별용으로 받음
    @NotBlank(message = "닉네임은 필수입니다.") String nickname,
    @NotNull Enums.Gender gender,
    @NotNull Enums.AgeGroup ageGroup,
    String job,
    String usagePurpose,
    LocalTime sleepTime,
    LocalTime wakeUpTime,
    @NotNull Enums.NotificationSetting notificationSetting
) {
}
