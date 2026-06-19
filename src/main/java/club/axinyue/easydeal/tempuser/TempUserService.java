package club.axinyue.easydeal.tempuser;

import club.axinyue.easydeal.config.AppSecurityProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class TempUserService {
    public static final String TOKEN_COOKIE_NAME = "easy_deal_token";
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(7);

    private final SecureRandom secureRandom = new SecureRandom();
    private final TempUserRepository tempUserRepository;
    private final UsernameProvider usernameProvider;
    private final AppSecurityProperties securityProperties;

    public TempUserService(
            TempUserRepository tempUserRepository,
            UsernameProvider usernameProvider,
            AppSecurityProperties securityProperties
    ) {
        this.tempUserRepository = tempUserRepository;
        this.usernameProvider = usernameProvider;
        this.securityProperties = securityProperties;
    }

    public TempUserSession getOrCreate(String token, Locale locale) {
        if (StringUtils.hasText(token)) {
            String tokenHash = hashToken(token);
            return tempUserRepository.findByTokenHash(tokenHash)
                    .map(tempUser -> new TempUserSession(TempUserResponse.from(tempUser), null))
                    .orElseGet(() -> create(locale));
        }
        return create(locale);
    }

    private TempUserSession create(Locale locale) {
        String token = generateToken();
        String localeKey = usernameProvider.resolveLocaleKey(locale);
        String username = usernameProvider.randomUsername(locale);
        TempUser tempUser = tempUserRepository.create(hashToken(token), username, localeKey);
        return new TempUserSession(TempUserResponse.from(tempUser), buildCookie(token));
    }

    public ResponseCookie buildCookie(String token) {
        return ResponseCookie.from(TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(securityProperties.isCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(COOKIE_MAX_AGE)
                .build();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
