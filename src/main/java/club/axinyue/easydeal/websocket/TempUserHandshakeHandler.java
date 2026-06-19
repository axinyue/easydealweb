package club.axinyue.easydeal.websocket;

import club.axinyue.easydeal.tempuser.TempUserHandshakeInterceptor;
import club.axinyue.easydeal.tempuser.TempUserResponse;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Component
public class TempUserHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        Object tempUser = attributes.get(TempUserHandshakeInterceptor.TEMP_USER_ATTRIBUTE);
        if (tempUser instanceof TempUserResponse response && response.id() != null) {
            return () -> "temp-user-" + response.id();
        }
        return () -> "temp-user-" + UUID.randomUUID();
    }
}
