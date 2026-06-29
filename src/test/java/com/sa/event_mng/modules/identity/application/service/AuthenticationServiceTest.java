package com.sa.event_mng.modules.identity.application.service;

import com.sa.event_mng.modules.identity.application.dto.request.AuthenticationRequest;
import com.sa.event_mng.modules.identity.domain.model.Role;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.InvalidatedTokenRepository;
import com.sa.event_mng.modules.identity.domain.repository.RoleRepository;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import com.sa.event_mng.shared.infrastructure.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService.SIGNER_KEY = "test-secret-key-for-jwt-hs512-generation-minimum-64-bytes-required!!";
        authenticationService.VALID_DURATION = 3600L;
        authenticationService.REFRESHABLE_DURATION = 86400L;
    }

    @Test
    void authenticate_shouldReturnToken_whenCredentialsAreValid() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("alice")
                .password("password123")
                .build();

        User user = User.builder()
                .username("alice")
                .password("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.builder().name("CUSTOMER").build()))
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        var response = authenticationService.authenticate(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isBlank());
        verify(userRepository).findByUsername("alice");
        verify(passwordEncoder).matches("password123", "encoded-password");
    }

    @Test
    void authenticate_shouldThrow_whenUserDoesNotExist() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("ghost")
                .password("password123")
                .build();

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authenticationService.authenticate(request));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void authenticate_shouldThrow_whenPasswordIsIncorrect() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("alice")
                .password("wrong-password")
                .build();

        User user = User.builder()
                .username("alice")
                .password("encoded-password")
                .enabled(true)
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authenticationService.authenticate(request));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    void authenticate_shouldThrow_whenAccountIsDisabled() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("alice")
                .password("password123")
                .build();

        User user = User.builder()
                .username("alice")
                .password("encoded-password")
                .enabled(false)
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authenticationService.authenticate(request));

        assertEquals(ErrorCode.USER_DISABLED, exception.getErrorCode());
    }
}
