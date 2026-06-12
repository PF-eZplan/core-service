package com.pathfinder.calbak.controller;

import com.pathfinder.calbak.dto.UserAdditionalInfoRequest;
import com.pathfinder.calbak.dto.UserNicknameUpdateRequest;
import com.pathfinder.calbak.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 온보딩 추가 정보 입력 API
    @PatchMapping("/additional-info")
    public ResponseEntity<String> updateAdditionalInfo(
        @Valid @RequestBody UserAdditionalInfoRequest request
    ) {
        userService.updateAdditionalInfo(request);
        return ResponseEntity.ok("추가 정보 등록이 완료되었습니다.");
    }

    // 닉네임 단건 수정 API
    @PatchMapping("/nickname")
    public ResponseEntity<String> updateNickname(
        @Valid @RequestBody UserNicknameUpdateRequest request
    ) {
        userService.updateNickname(request);
        return ResponseEntity.ok("닉네임이 성공적으로 변경되었습니다.");
    }
}
