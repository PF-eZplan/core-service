package com.pathfinder.calbak.dto;

import com.pathfinder.calbak.domain.enums.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record UserAdditionalInfoRequest(
    @NotBlank(message = "닉네임은 필수입니다.") String nickname,
    @NotNull Enums.Gender gender,
    @NotNull Enums.AgeGroup ageGroup,
    String job,
    String usagePurpose,
    LocalTime sleepTime,
    LocalTime wakeUpTime,
    @NotNull Enums.NotificationStatus notificationStatus
) {
}
