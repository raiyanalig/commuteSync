package com.commutesync.auth.service;

import com.commutesync.auth.domain.User;
import com.commutesync.auth.dto.AuthResponse;
import com.commutesync.auth.dto.LoginRequest;
import com.commutesync.auth.dto.RegisterRequest;
import com.commutesync.auth.dto.UserResponse;
import com.commutesync.auth.repository.UserRepository;
import com.commutesync.auth.security.JwtService;
import com.commutesync.auth.security.UserPrincipal;
import com.commutesync.audit.domain.AuditAction;
import com.commutesync.audit.service.AuditService;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setEnabled(true);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(UserPrincipal.from(saved));
        return AuthResponse.of(token, jwtService.getExpirationMs(), UserResponse.from(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt for {}", email);
            auditService.record(AuditAction.LOGIN_FAILED, email, "USER", null, "Failed login attempt");
            throw ex;
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        auditService.record(AuditAction.LOGIN, email, "USER", principal.getUser().getId(), "User logged in");
        return AuthResponse.of(token, jwtService.getExpirationMs(), UserResponse.from(principal.getUser()));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return UserResponse.from(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
