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

        // HttpOnly 쿠키로 JWT 전달
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(accessTokenExpiry / 1000)
            .sameSite("Lax")
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 토큰은 쿠키에 담겼으므로 URL에 노출 없이 리다이렉트
        String targetUrl = frontendUrl + "/oauth-redirect";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
