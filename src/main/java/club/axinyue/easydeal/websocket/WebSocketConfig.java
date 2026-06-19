package club.axinyue.easydeal.websocket;

import club.axinyue.easydeal.config.AppSecurityProperties;
import club.axinyue.easydeal.tempuser.TempUserHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final TempUserHandshakeInterceptor tempUserHandshakeInterceptor;
    private final TempUserHandshakeHandler tempUserHandshakeHandler;
    private final AppSecurityProperties securityProperties;

    public WebSocketConfig(
            TempUserHandshakeInterceptor tempUserHandshakeInterceptor,
            TempUserHandshakeHandler tempUserHandshakeHandler,
            AppSecurityProperties securityProperties
    ) {
        this.tempUserHandshakeInterceptor = tempUserHandshakeInterceptor;
        this.tempUserHandshakeHandler = tempUserHandshakeHandler;
        this.securityProperties = securityProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(securityProperties.getAllowedOrigins())
                .setHandshakeHandler(tempUserHandshakeHandler)
                .addInterceptors(tempUserHandshakeInterceptor);

        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns(securityProperties.getAllowedOrigins())
                .setHandshakeHandler(tempUserHandshakeHandler)
                .addInterceptors(tempUserHandshakeInterceptor)
                .withSockJS();
    }
}
