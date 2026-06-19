package club.axinyue.easydeal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

@Configuration
@EnableConfigurationProperties(AppSecurityProperties.class)
public class WebSecurityConfig implements WebMvcConfigurer {
    private final AppSecurityProperties securityProperties;

    public WebSecurityConfig(AppSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(securityProperties.getAllowedOrigins())
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "Accept-Language")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter(securityProperties);
    }

    public static class SecurityHeadersFilter extends OncePerRequestFilter {
        private final AppSecurityProperties securityProperties;

        public SecurityHeadersFilter(AppSecurityProperties securityProperties) {
            this.securityProperties = securityProperties;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            if (!isAllowedHost(request)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host");
                return;
            }
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("Referrer-Policy", "same-origin");
            response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            if (isSecureRequest(request)) {
                response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            }
            filterChain.doFilter(request, response);
        }

        private boolean isSecureRequest(HttpServletRequest request) {
            return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        }

        private boolean isAllowedHost(HttpServletRequest request) {
            String[] allowedHosts = securityProperties.getAllowedHosts();
            if (allowedHosts == null || allowedHosts.length == 0) {
                return true;
            }
            String host = request.getHeader("Host");
            if (host == null || host.isBlank()) {
                return false;
            }
            String normalizedHost = host.split(":", 2)[0].toLowerCase(Locale.ROOT);
            return Arrays.stream(allowedHosts)
                    .map(allowedHost -> allowedHost.toLowerCase(Locale.ROOT))
                    .anyMatch(normalizedHost::equals);
        }
    }
}
