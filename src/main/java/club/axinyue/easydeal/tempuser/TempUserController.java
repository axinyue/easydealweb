package club.axinyue.easydeal.tempuser;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.List;

@RestController
@RequestMapping("/api/temp-users")
public class TempUserController {
    private final TempUserService tempUserService;

    public TempUserController(TempUserService tempUserService) {
        this.tempUserService = tempUserService;
    }

    @PostMapping
    public TempUserResponse createIfAbsent(
            @CookieValue(value = TempUserService.TOKEN_COOKIE_NAME, required = false) String token,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        TempUserSession session = tempUserService.getOrCreate(token, resolveLocale(acceptLanguage));
        if (session.hasNewCookie()) {
            response.addHeader(HttpHeaders.SET_COOKIE, session.cookie().toString());
        }
        return session.tempUser();
    }

    private Locale resolveLocale(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return Locale.CHINESE;
        }

        try {
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
            if (ranges.isEmpty()) {
                return Locale.CHINESE;
            }
            return Locale.forLanguageTag(ranges.get(0).getRange());
        } catch (IllegalArgumentException e) {
            return Locale.CHINESE;
        }
    }
}
