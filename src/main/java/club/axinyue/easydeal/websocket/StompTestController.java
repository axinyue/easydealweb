package club.axinyue.easydeal.websocket;

import club.axinyue.easydeal.tempuser.TempUserHandshakeInterceptor;
import club.axinyue.easydeal.tempuser.TempUserResponse;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

@Controller
public class StompTestController {
    @MessageMapping("/test")
    @SendTo("/topic/test")
    public StompTestMessage test(
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        return new StompTestMessage(
                readMessage(payload),
                payload,
                headerAccessor.getSessionId(),
                readTempUser(headerAccessor),
                Instant.now().toString()
        );
    }

    private String readMessage(Map<String, Object> payload) {
        Object message = payload.get("message");
        return message == null ? "" : String.valueOf(message);
    }

    private TempUserResponse readTempUser(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }

        Object tempUser = sessionAttributes.get(TempUserHandshakeInterceptor.TEMP_USER_ATTRIBUTE);
        if (tempUser instanceof TempUserResponse response) {
            return response;
        }
        return null;
    }
}
