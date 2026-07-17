package org.example.batuku.utils;

import org.example.batuku.services.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtRequestFilter jwtRequestFilter;
    private final CustomAuthenticationManager customAuthenticationManager;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @Value("${batuku.cors.allowed-origin}")
    private String allowedOrigin;

    public WebSecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                             JwtRequestFilter jwtRequestFilter,
                             CustomAuthenticationManager customAuthenticationManager,
                             CustomOAuth2UserService customOAuth2UserService,
                             OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
                             OAuth2AuthenticationFailureHandler oAuth2FailureHandler) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtRequestFilter = jwtRequestFilter;
        this.customAuthenticationManager = customAuthenticationManager;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return customAuthenticationManager;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            // IF_REQUIRED: necessário para o fluxo OAuth2 (state param) — as chamadas à API continuam a usar JWT
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/authenticate",
                    "/api/auth/register",
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tracks", "/api/tracks/artist/**", "/api/tracks/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/follows/followers/**", "/api/follows/following/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/**", "/api/artists/**", "/api/search").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/discord/events").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            );

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
