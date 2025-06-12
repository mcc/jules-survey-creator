package mcc.survey.creator.security;

import mcc.survey.creator.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private final String testSecret = "testSecretKeyForJwtGenerationWhichIsVeryLongAndSecureAndAtLeast64Chars"; // 64 chars
    private final long testExpirationMs = 3600000; // 1 hour
    private final long testRefreshExpirationMs = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", testExpirationMs);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMs", testRefreshExpirationMs);
        jwtTokenProvider.init(); // Manually call init to set up the key
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        Set<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(Role.ROLE_USER.name()));
        Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", null, authorities);

        String token = jwtTokenProvider.generateToken(authentication);
        assertNotNull(token);

        // Re-extracting key for validation as it's private. Better to have a getter or make it package-private for tests.
        // For simplicity, this test will rely on the provider's own validation method.
        // To properly parse claims, we would need access to the key.
        // Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        // assertEquals("testuser", claims.getSubject());

        assertTrue(jwtTokenProvider.validateToken(token)); // Basic validation
        assertEquals("testuser", jwtTokenProvider.getUsernameFromJWT(token));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        Set<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(Role.ROLE_USER.name()));
        Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", null, authorities);
        String token = jwtTokenProvider.generateToken(authentication);

        UserDetails userDetails = new User("testuser", "", authorities);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

        assertTrue(jwtTokenProvider.validateToken(token));
        Authentication auth = jwtTokenProvider.getAuthentication(token);
        assertNotNull(auth);
        assertEquals("testuser", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .map(ga -> ga.getAuthority())
                .collect(Collectors.toList()).contains(Role.ROLE_USER.name()));
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() throws InterruptedException {
         ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 1L); // 1 ms
         // No need to call init() again if the key itself doesn't change, only expiration.
         // However, if key generation depends on expiration time (it doesn't here), it would be needed.

        Set<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(Role.ROLE_USER.name()));
        Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", null, authorities);
        String token = jwtTokenProvider.generateToken(authentication);

        Thread.sleep(50); // Wait for token to expire

        assertFalse(jwtTokenProvider.validateToken(token));
    }
}
