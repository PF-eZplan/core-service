package com.pathfinder.calbak.config;

import com.pathfinder.calbak.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    // @Lazy: JPA 초기화 완료 후 UserRepository를 주입받아 순환 의존성 방지
    @Bean
    public AuditorAware<UUID> auditorProvider(@Lazy UserRepository userRepository) {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
                return Optional.empty();
            }

            String email = authentication.getName();
            return userRepository.findByEmail(email).map(user -> user.getId());
        };
    }
}
