package com.pathfinder.calbak.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    // 프론트엔드 URL이 아닌 '리다이렉트 전용 URL' 사용
    @Value("${app.oauth.redirect-url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // JWT 토큰 생성
        String token = jwtProvider.generateAccessToken(email);

        // 앱(Expo) 환경을 위해 쿠키 대신 딥링크 URL 파라미터로 토큰 전달 - 예: exp://192.168.0.5:8081?token=eyJhbGciOi...
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
            .queryParam("token", token)
            .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
