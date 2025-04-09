package com.enit.satellite_platform.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.enit.satellite_platform.config.rateLimiter.RateLimitingFilter;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.Jwt.JwtAuthenticationFilter;

import org.springframework.web.client.RestTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import java.util.Arrays;

/**
 * Configuration class for Spring Security.
 * Defines security settings, including CORS configuration, CSRF protection, authorization rules,
 * session management, and filter ordering.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configures the security filter chain.
     *
     * @param http The HttpSecurity object to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS using the defined corsConfigurationSource
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF as this is a stateless JWT-based application
                .csrf(csrf -> csrf.disable())
                // Configure authorization rules
                .authorizeHttpRequests(requests -> requests
                        // Allow unauthenticated access to authentication endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        // Allow unauthenticated access to WebSocket handshake/info endpoints
                        .requestMatchers("/ws-logs/**").permitAll() // Keep existing rule for audit logs if needed
                        .requestMatchers("/monitoring-websocket/**").permitAll() // Add permission for monitoring endpoint
                        // Restrict DELETE requests to /api/account/** to ADMIN role
                        .requestMatchers(HttpMethod.DELETE, "/api/account/**").hasRole("ADMIN")
                        // Restrict admin endpoints (including audit, roles, etc.) to users with ROLE_ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Allow unauthenticated access to the Prometheus scraping endpoint
                        .requestMatchers("/actuator/prometheus").permitAll()
                        // Restrict other Actuator endpoints to users with ROLE_ADMIN
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // Restrict user endpoints to users with ROLE_THEMATICIAN
                        .requestMatchers("/api/thematician/**").hasRole("THEMATICIAN")
                        // Require authentication for all other requests
                        .anyRequest().authenticated())
                // Set session management to stateless for JWT
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Add JWT filter first to populate SecurityContext
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // Add Rate Limiting filter after JWT filter
        http.addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures the CORS configuration source.
     *
     * @return The CorsConfigurationSource.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Specify the frontend origin explicitly instead of wildcard when using credentials
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // Allow frontend dev server
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "content-type", "x-requested-with")); // Added common headers
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true); // Explicitly set allowCredentials
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Provides a BCryptPasswordEncoder bean for password encoding.
     *
     * @return The BCryptPasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides the AuthenticationManager bean.
     *
     * @param authenticationConfiguration The AuthenticationConfiguration.
     * @return The AuthenticationManager instance.
     * @throws Exception If an error occurs while getting the AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Provides a RestTemplate bean for making HTTP requests.
     *
     * @return The RestTemplate instance.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Provides a RedisTemplate bean for interacting with Redis.
     *
     * @param redisConnectionFactory The RedisConnectionFactory.
     * @return The RedisTemplate instance.
     */
    @Bean
public RedisTemplate<String, Object> securityRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        return template;
    }
}
