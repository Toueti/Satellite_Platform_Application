package com.enit.satellite_platform.config.webSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to carry messages back to the client on destinations prefixed with "/topic"
        config.enableSimpleBroker("/topic");
        // Designates the "/app" prefix for messages that are bound for methods annotated with @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws-logs" endpoint, enabling SockJS fallback options so that alternate transports may be used if WebSocket is not available.
        // SockJS is used to enable fallback options for browsers that don’t support WebSocket.
        // Allow all origins for simplicity during development. Adjust for production.
        registry.addEndpoint("/ws-logs").setAllowedOriginPatterns("*").withSockJS();
    }
}
