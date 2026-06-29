package com.sa.event_mng.modules.identity.application.service;

import com.sa.event_mng.modules.identity.application.dto.request.UserUpdateRequest;
import com.sa.event_mng.modules.identity.application.dto.response.UserResponse;
import com.sa.event_mng.modules.identity.application.mapper.UserMapper;
import com.sa.event_mng.modules.identity.domain.model.Role;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.RoleRepository;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import com.sa.event_mng.shared.infrastructure.email.EmailService;
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
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RoleRepository roleRepository;
    @Mock EmailService emailService;

    @InjectMocks UserService userService;

    @Test
    void getUserByUsername_shouldReturnUserResponse_whenUserExists() {
        User user = User.builder().username("alice").enabled(true).build();
        UserResponse expected = UserResponse.builder().username("alice").build();

        when(userRepository.findByUsernameAndEnabledTrue("alice")).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(expected);

        UserResponse result = userService.getUserByUsername("alice");

        assertEquals("alice", result.getUsername());
    }

    @Test
    void getUserByUsername_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsernameAndEnabledTrue("ghost")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> userService.getUserByUsername("ghost"));
        assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
    }

    @Test
    void updateUser_shouldThrow_whenUserDisabled() {
        User user = User.builder().username("alice").enabled(false).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        AppException ex = assertThrows(AppException.class,
                () -> userService.updateUser("alice", new UserUpdateRequest()));
        assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
    }

    @Test
    void updateUser_shouldThrow_whenEmailAlreadyTaken() {
        User user = User.builder().username("alice").email("old@email.com").enabled(true).build();
        UserUpdateRequest request = UserUpdateRequest.builder().email("taken@email.com").build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndEnabledTrue("taken@email.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> userService.updateUser("alice", request));
        assertEquals(ErrorCode.EMAIL_EXISTED, ex.getErrorCode());
    }

    @Test
    void updateUser_shouldSaveAndReturn_whenRequestIsValid() {
        User user = User.builder().username("alice").email("old@email.com").enabled(true).build();
        UserUpdateRequest request = UserUpdateRequest.builder().fullName("Alice New").build();
        UserResponse expected = UserResponse.builder().username("alice").build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(expected);

        UserResponse result = userService.updateUser("alice", request);

        assertNotNull(result);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_shouldThrow_whenUserIsStaff() {
        User user = User.builder().username("staff1").enabled(true)
                .roles(Set.of(Role.builder().name("STAFF").build())).build();

        when(userRepository.findByUsernameAndEnabledTrue("staff1")).thenReturn(Optional.of(user));

        AppException ex = assertThrows(AppException.class, () -> userService.deleteUser("staff1"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void deleteUser_shouldDisableUser_whenUserIsCustomer() {
        User user = User.builder().username("customer1").enabled(true)
                .roles(Set.of(Role.builder().name("CUSTOMER").build())).build();

        when(userRepository.findByUsernameAndEnabledTrue("customer1")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        String result = userService.deleteUser("customer1");

        assertEquals("User disabled", result);
        assertFalse(user.isEnabled());
    }
}
