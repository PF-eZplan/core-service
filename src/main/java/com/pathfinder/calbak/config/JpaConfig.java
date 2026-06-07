// JpaConfig.java
package com.pathfinder.calbak.config;

import com.pathfinder.calbak.security.AuthenticatedUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    // DB 조회 없이 SecurityContext의 AuthenticatedUser에서 UUID를 꺼냄 -> 트랜잭션 flush 중 DB 쿼리로 인한 "Could not commit JPA transaction" 방지
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
                return Optional.empty();
            }

            // JwtAuthenticationFilter에서 이미 AuthenticatedUser로 저장해둔 값을 꺼냄
            if (authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
                return Optional.of(authenticatedUser.id());
            }

            return Optional.empty();
        };
    }
}
