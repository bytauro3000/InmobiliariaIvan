package com.Inmobiliaria.demo.config;

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
		
		config.enableSimpleBroker("/grupal", "/privado");
		config.setApplicationDestinationPrefixes("/backend");
		config.setUserDestinationPrefix("/usuario");
		
	}
	
	
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {

		registry.addEndpoint("/chat")
		.setAllowedOriginPatterns("*")
		.withSockJS();

		registry.addEndpoint("/chat-ws")
		.setAllowedOriginPatterns("*");
	}
	
}
