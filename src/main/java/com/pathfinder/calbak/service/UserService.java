package com.pathfinder.calbak.service;

import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.dto.UserAdditionalInfoRequest;
import com.pathfinder.calbak.dto.UserNicknameUpdateRequest;
import com.pathfinder.calbak.exception.DuplicateNicknameException;
import com.pathfinder.calbak.exception.UserNotFoundException;
import com.pathfinder.calbak.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updateAdditionalInfo(UserAdditionalInfoRequest request) {
        // 1. 유저 조회
        // JwtAuthenticationFilter가 SecurityContext에 등록한 이메일 추출
        String email = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow(UserNotFoundException::new);

        // 2. 닉네임 중복 검증 (자신의 기존 닉네임과 다를 때만 검사)
        if (!request.nickname().equals(user.getNickname()) && userRepository.existsByNickname(request.nickname())) {
            throw new DuplicateNicknameException();
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
            request.notificationStatus()
        );

        // 동시에 같은 닉네임으로 저장될 때 발생하는 DB Unique 제약 에러 방어
        flushAndCatchDuplicate();
    }

    @Transactional
    public void updateNickname(UserNicknameUpdateRequest request) {
        // 1. 현재 접속 중인 유저 조회
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(UserNotFoundException::new);

        String newNickname = request.nickname();

        // 2. 현재 닉네임과 동일하게 변경하려고 하면 아무 작업 없이 리턴 (DB 쿼리 절약)
        if (newNickname.equals(user.getNickname())) {
            return;
        }

        // 3. 중복 검사 (새로운 닉네임이 이미 존재하는지)
        if (userRepository.existsByNickname(newNickname)) {
            throw new DuplicateNicknameException();
        }

        // 4. 닉네임 업데이트 (Dirty Checking으로 인해 자동 UPDATE 쿼리 발생)
        user.updateNickname(newNickname);

        // 동시에 같은 닉네임으로 저장될 때 발생하는 DB Unique 제약 에러 방어
        flushAndCatchDuplicate();
    }

    // 중복되는 DB Unique 제약 예외 처리 로직 공통화
    private void flushAndCatchDuplicate() {
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateNicknameException();
        }
    }
}
