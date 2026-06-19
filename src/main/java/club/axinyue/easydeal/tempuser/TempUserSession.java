package club.axinyue.easydeal.tempuser;

import org.springframework.http.ResponseCookie;

public record TempUserSession(
        TempUserResponse tempUser,
        ResponseCookie cookie
) {
    public boolean hasNewCookie() {
        return cookie != null;
    }
}
