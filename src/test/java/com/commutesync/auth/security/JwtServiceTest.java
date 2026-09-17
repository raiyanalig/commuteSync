package com.commutesync.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.commutesync.auth.domain.Role;
import com.commutesync.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-commutesync-jwt-unit-tests-1234567890";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    @Test
    void generatesTokenWithSubject() {
        String token = jwtService.generateToken(userDetails("raiyan@example.com", Role.EMPLOYEE));

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("raiyan@example.com");
    }

    @Test
    void tokenIsValidForMatchingUser() {
        String token = jwtService.generateToken(userDetails("raiyan@example.com", Role.EMPLOYEE));

        assertThat(jwtService.isTokenValid(token, userDetails("raiyan@example.com", Role.EMPLOYEE))).isTrue();
    }

    @Test
    void tokenIsInvalidForDifferentUser() {
        String token = jwtService.generateToken(userDetails("raiyan@example.com", Role.EMPLOYEE));

        assertThat(jwtService.isTokenValid(token, userDetails("other@example.com", Role.EMPLOYEE))).isFalse();
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtService shortLivedJwtService = new JwtService(SECRET, -1_000L);
        String token = shortLivedJwtService.generateToken(userDetails("raiyan@example.com", Role.EMPLOYEE));

        assertThat(shortLivedJwtService.isTokenValid(token, userDetails("raiyan@example.com", Role.EMPLOYEE)))
                .isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(userDetails("raiyan@example.com", Role.EMPLOYEE));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.isTokenValid(tampered, userDetails("raiyan@example.com", Role.EMPLOYEE))).isFalse();
    }

    private UserDetails userDetails(String email, Role role) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setPassword("hashed");
        user.setRole(role);
        user.setEnabled(true);
        return UserPrincipal.from(user);
    }
}
