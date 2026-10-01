package org.example.batuku.utils;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.batuku.domain.User;
import org.example.batuku.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class TokenRevocationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;

    public TokenRevocationFilter(JwtTokenUtil jwtTokenUtil, UserRepository userRepository) {
        this.jwtTokenUtil   = jwtTokenUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/ws/") || uri.equals("/ws");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null
                || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Date issuedAt = jwtTokenUtil.getClaimFromToken(token, Claims::getIssuedAt);
            String email  = jwtTokenUtil.getUsernameFromToken(token);

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                chain.doFilter(request, response);
                return;
            }

            LocalDateTime tokenIat = issuedAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime validFrom = user.getTokenValidFrom();

            if (validFrom != null && tokenIat.isBefore(validFrom)) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"TOKEN_REVOKED\"}");
                return;
            }
        } catch (Exception e) {
            // Token inválido já foi tratado pelo JwtRequestFilter
        }

        chain.doFilter(request, response);
    }
}
