package com.pathfinder.calbak.service;

import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.dto.UserAdditionalInfoRequest;
import com.pathfinder.calbak.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updateAdditionalInfo(UserAdditionalInfoRequest request) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 2. 닉네임 중복 검증 (자신의 기존 닉네임과 다를 때만 검사)
        if (!request.nickname().equals(user.getNickname()) && userRepository.existsByNickname(request.nickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 3. 엔티티에 만들어둔 온보딩 편의 메서드 호출
        user.completeOnboarding(
            request.nickname(),
            request.gender(),
            request.ageGroup(),
            request.job(),
            request.usagePurpose(),
            request.sleepTime(),
            request.wakeUpTime(),
            request.notificationSetting()
        );
    }
}
