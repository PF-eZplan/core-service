package com.pathfinder.calbak.security;

import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 쿠키 토큰을 먼저 검사하고, 없거나 만료되었으면 헤더로 Fallback
        String token = extractTokenFromCookie(request);
        boolean isValid = token != null && jwtProvider.validateToken(token);

        if (!isValid) {
            token = extractTokenFromHeader(request);
            isValid = token != null && jwtProvider.validateToken(token);
        }

        // 유효한 토큰이 있으면 SecurityContext에 인증 정보 등록
        if (isValid) {
            String email = jwtProvider.getEmailFromToken(token);

            // 필터 단계에서 DB를 조회해 UUID를 가져옴 -> AuditorAware에서 DB 조회 불필요 (트랜잭션 flush 중 DB 쿼리 문제 방지)
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                AuthenticatedUser authenticatedUser =
                    new AuthenticatedUser(userOptional.get().getId(), email);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        authenticatedUser, // Principal: AuthenticatedUser 객체
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
            .filter(cookie -> "access_token".equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
