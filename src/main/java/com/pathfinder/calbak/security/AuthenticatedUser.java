package com.pathfinder.calbak.security;

import java.security.Principal;
import java.util.UUID;

// JWT 인증 후 SecurityContext에 저장되는 사용자 정보
// Principal을 구현해 authentication.getName()이 email을 반환하도록 함 -> 기존 컨트롤러의 authentication.getName() 코드 변경 불필요
public record AuthenticatedUser(UUID id, String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
