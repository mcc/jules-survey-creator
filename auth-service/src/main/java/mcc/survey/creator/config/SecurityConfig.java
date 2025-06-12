package mcc.survey.creator.config;

import mcc.survey.creator.model.Role;
import mcc.survey.creator.security.JwtTokenFilter;
import mcc.survey.creator.security.JwtTokenProvider;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enables @PreAuthorize, @PostAuthorize
public class SecurityConfig {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/login", "/api/auth/signup", "/api/auth/refresh").permitAll() // Specific public auth endpoints
                .requestMatchers("/api/admin/**").authenticated() // Secure admin endpoints
                .requestMatchers("/api/auth/users/**").authenticated() // Secure user-specific info under /api/auth/users
                .requestMatchers("/api/surveys/**").authenticated() // Surveys endpoints require authentication
                .requestMatchers("/h2-console/**").permitAll() // Allow H2 console
                .anyRequest().authenticated() // All other requests need authentication
            )
            .addFilterBefore(new JwtTokenFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        // For H2 console to work properly with Spring Security
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        System.out.println(allowedOrigins[0]);
        CorsConfiguration configuration = new CorsConfiguration();
            configuration.addAllowedMethod("GET");
            configuration.addAllowedMethod("POST");
            configuration.addAllowedMethod("PUT");
            configuration.addAllowedMethod("DELETE");
            configuration.addAllowedMethod("OPTIONS");
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
            configuration.addAllowedHeader("*");
            configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        // If you want to allow specific headers, you can add them like this:
        return source;
    }
    
}
