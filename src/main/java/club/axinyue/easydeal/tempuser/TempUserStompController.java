package club.axinyue.easydeal.tempuser;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class TempUserStompController {
    @MessageMapping("/temp-users")
    @SendToUser("/queue/temp-users")
    public TempUserResponse currentTempUser(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new IllegalStateException("Websocket session attributes are missing.");
        }

        Object tempUser = sessionAttributes
                .get(TempUserHandshakeInterceptor.TEMP_USER_ATTRIBUTE);
        if (tempUser instanceof TempUserResponse response) {
            return response;
        }
        throw new IllegalStateException("Temp user is missing from websocket session.");
    }
}
