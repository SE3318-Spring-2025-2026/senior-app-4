package com.spms.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * P5.7 — WebSocket Configuration
 *
 * Endpoint: ws://localhost:8080/ws  (SockJS fallback da desteklenir)
 * Push topic: /user/{userId}/queue/notifications
 *
 * Frontend bağlanmak için:
 *   new SockJS('http://localhost:8080/ws')   veya
 *   new WebSocket('ws://localhost:8080/ws')
 *
 * Bilgisayarına ekstra kurulum GEREKMEZ. Sadece Maven bağımlılığı yeterlidir.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker: /topic (broadcast) ve /queue (user-specific)
        registry.enableSimpleBroker("/topic", "/queue");
        // Client'tan sunucuya mesaj prefix'i
        registry.setApplicationDestinationPrefixes("/app");
        // User-specific destination prefix  → /user/{userId}/queue/...
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:3000",
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                )
                .withSockJS();  // SockJS fallback (HTTP polling tarayıcılar için)
    }
}
