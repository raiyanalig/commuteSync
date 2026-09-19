package com.commutesync.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.auth.domain.Role;
import com.commutesync.auth.domain.User;
import com.commutesync.auth.dto.AuthResponse;
import com.commutesync.auth.dto.LoginRequest;
import com.commutesync.auth.dto.RegisterRequest;
import com.commutesync.auth.dto.UserResponse;
import com.commutesync.auth.repository.UserRepository;
import com.commutesync.auth.security.JwtService;
import com.commutesync.auth.security.UserPrincipal;
import com.commutesync.audit.service.AuditService;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Raiyan", "Raiyan@Example.com", "password123", Role.EMPLOYEE);
        when(userRepository.existsByEmail("raiyan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerEncodesPasswordAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("Raiyan", "Raiyan@Example.com", "password123", Role.EMPLOYEE);
        when(userRepository.existsByEmail("raiyan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("raiyan@example.com");
        assertThat(response.user().role()).isEqualTo(Role.EMPLOYEE);
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = user(1L, "raiyan@example.com", Role.ADMIN);
        UserPrincipal principal = UserPrincipal.from(user);
        Authentication authenticated =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

        AuthResponse response = authService.login(new LoginRequest("raiyan@example.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("raiyan@example.com");
    }

    @Test
    void loginPropagatesBadCredentials() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("raiyan@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void getCurrentUserReturnsProfile() {
        when(userRepository.findByEmail("raiyan@example.com"))
                .thenReturn(Optional.of(user(1L, "raiyan@example.com", Role.EMPLOYEE)));

        UserResponse response = authService.getCurrentUser("raiyan@example.com");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.role()).isEqualTo(Role.EMPLOYEE);
    }

    @Test
    void getCurrentUserThrowsWhenMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private User user(Long id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setFullName("Raiyan Ali");
        user.setEmail(email);
        user.setPassword("hashed-password");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}
