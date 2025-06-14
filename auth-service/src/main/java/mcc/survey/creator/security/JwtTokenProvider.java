package mcc.survey.creator.security;

import mcc.survey.creator.model.Role;
import mcc.survey.creator.service.ConfigurationService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {
    private Logger logger = org.slf4j.LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private Key key;

    private final UserDetailsService userDetailsService;
    private final ConfigurationService configurationService;

    public JwtTokenProvider(UserDetailsService userDetailsService, ConfigurationService configurationService) {
        this.userDetailsService = userDetailsService;
        this.configurationService = configurationService;
    }

    @PostConstruct
    protected void init() {
        String jwtSecretString = configurationService.getRequiredConfigValue("app.jwt.secret");
        key = Keys.hmacShaKeyFor(jwtSecretString.getBytes());
    }

    public String generateToken(Authentication authentication) {
        logger.info("Generating JWT token for user: " + authentication.getName());
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication object is null or not authenticated");
        }
        String username = authentication.getName();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        authentication.getAuthorities().stream()
                .collect(Collectors.toSet());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (Exception ex) {
            logger.error("Invalid JWT token: " + ex.getMessage(), ex);
            // Log error
        } 
        return false;
    }

    public Authentication getAuthentication(String token) {
        logger.info("token: " + token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(getUsernameFromJWT(token));
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        List<String> roles = claims.get("roles", List.class);
        logger.info("User roles: " + String.join(",", roles));
        Set<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    public String resolveToken(HttpServletRequest req) {
        logger.info("resolveToken: " + req.getContextPath());
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
