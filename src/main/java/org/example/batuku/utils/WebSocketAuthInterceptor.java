package org.example.batuku.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(JwtTokenUtil jwtTokenUtil, JwtUserDetailsService userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            log.warn("[WS] preSend command={}", accessor.getCommand());
        }

        if (accessor != null && (StompCommand.CONNECT.equals(accessor.getCommand())
                || StompCommand.STOMP.equals(accessor.getCommand()))) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String email = jwtTokenUtil.getUsernameFromToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    if (jwtTokenUtil.validateToken(token, userDetails)) {
                        accessor.setUser(new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()));
                        log.info("[WS] STOMP CONNECT autenticado: {}", email);
                    } else {
                        log.warn("[WS] STOMP CONNECT token inválido para: {}", email);
                    }
                } catch (Exception e) {
                    log.warn("[WS] STOMP CONNECT falhou: {}", e.getMessage());
                }
            } else {
                log.warn("[WS] STOMP CONNECT sem token Authorization");
            }
        }
        return message;
    }
}
