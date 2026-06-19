package club.axinyue.easydeal.websocket;

import club.axinyue.easydeal.tempuser.TempUserResponse;

import java.util.Map;

public record StompTestMessage(
        String message,
        Map<String, Object> payload,
        String sessionId,
        TempUserResponse tempUser,
        String serverTime
) {
}
