package com.sa.event_mng.modules.identity.presentation.controller;

import com.sa.event_mng.modules.identity.application.dto.request.*;
import com.sa.event_mng.modules.identity.application.dto.response.AuthenticationResponse;
import com.sa.event_mng.modules.identity.application.dto.response.IntrospectResponse;
import com.sa.event_mng.modules.identity.application.service.AuthenticationService;
import com.sa.event_mng.shared.dto.ApiResponse;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Xác thực", description = "Đăng nhập, đăng ký và quản lý phiên làm việc")
public class AuthenticationController {

    AuthenticationService authenticationService;

    @Operation(summary = "Đăng nhập")
    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }

    @Operation(summary = "Đăng ký tài khoản mới")
    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody @Valid UserCreateRequest request) {
        var result = authenticationService.register(request);
        return ApiResponse.<String>builder().result(result).build();
    }

    @Operation(summary = "Xác thực Email")
    @GetMapping(value = "/verify", produces = "text/html;charset=UTF-8")
    public String verifyEmail(@RequestParam String token) {
        return authenticationService.verifyEmail(token);
    }

    @Operation(summary = "Yêu cầu mã OTP lấy lại mật khẩu")
    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        var result = authenticationService.forgotPassword(request);
        return ApiResponse.<String>builder().result(result).build();
    }

    @Operation(summary = "Đặt lại mật khẩu với mã OTP")
    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        var result = authenticationService.resetPassword(request);
        return ApiResponse.<String>builder().result(result).build();
    }

    @Operation(summary = "Organizer đăng ký tài khoản cho Staff")
    @PostMapping("/register-staff")
    public ApiResponse<String> registerStaff(@RequestBody @Valid StaffCreateRequest request) {
        var result = authenticationService.registerStaff(request);
        return ApiResponse.<String>builder().result(result).build();
    }

    @Operation(summary = "Kiểm tra token")
    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws JOSEException, ParseException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder().result(result).build();
    }

    @Operation(summary = "Làm mới token")
    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> build(@RequestBody RefreshRequest request)
            throws JOSEException, ParseException {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }

    @Operation(summary = "Đăng xuất")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request) throws JOSEException, ParseException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder().build();
    }
}
