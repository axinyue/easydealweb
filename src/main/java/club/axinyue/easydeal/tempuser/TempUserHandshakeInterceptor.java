package club.axinyue.easydeal.tempuser;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class TempUserHandshakeInterceptor implements HandshakeInterceptor {
    public static final String TEMP_USER_ATTRIBUTE = "tempUser";

    private final TempUserService tempUserService;

    public TempUserHandshakeInterceptor(TempUserService tempUserService) {
        this.tempUserService = tempUserService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        TempUserSession session = tempUserService.getOrCreate(
                readToken(request.getHeaders()),
                resolveLocale(request.getHeaders())
        );
        attributes.put(TEMP_USER_ATTRIBUTE, session.tempUser());
        if (session.hasNewCookie()) {
            response.getHeaders().add(HttpHeaders.SET_COOKIE, session.cookie().toString());
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private String readToken(HttpHeaders headers) {
        List<String> cookies = headers.get(HttpHeaders.COOKIE);
        if (cookies == null) {
            return null;
        }

        for (String cookieHeader : cookies) {
            String[] cookiesInHeader = cookieHeader.split(";");
            for (String cookie : cookiesInHeader) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && TempUserService.TOKEN_COOKIE_NAME.equals(parts[0])) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    private Locale resolveLocale(HttpHeaders headers) {
        List<Locale.LanguageRange> ranges = headers.getAcceptLanguage();
        if (ranges.isEmpty()) {
            return Locale.CHINESE;
        }
        return Locale.forLanguageTag(ranges.get(0).getRange());
    }
}
