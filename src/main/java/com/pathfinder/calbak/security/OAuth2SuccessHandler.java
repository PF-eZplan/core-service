package com.pathfinder.calbak.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // 1. 로그인 인증 토큰 (access_token) 생성 및 쿠키 세팅
        String token = jwtProvider.generateAccessToken(email);

        // HttpOnly 쿠키로 JWT 전달
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(accessTokenExpiry / 1000)
            .sameSite(cookieSameSite)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 2. Spring Security 6의 CSRF 지연 로딩 강제 활성화
        // 이 코드가 실행되어야만 브라우저에 XSRF-TOKEN 쿠키가 구워짐
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }

        // 3. 리다이렉트
        String targetUrl = frontendUrl + "/oauth-redirect";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
