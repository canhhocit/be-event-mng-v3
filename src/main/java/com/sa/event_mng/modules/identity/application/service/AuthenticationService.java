package com.sa.event_mng.modules.identity.application.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sa.event_mng.modules.identity.application.dto.request.*;
import com.sa.event_mng.modules.identity.application.dto.response.AuthenticationResponse;
import com.sa.event_mng.modules.identity.application.dto.response.IntrospectResponse;
import com.sa.event_mng.modules.identity.domain.model.InvalidatedToken;
import com.sa.event_mng.modules.identity.domain.model.Role;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.InvalidatedTokenRepository;
import com.sa.event_mng.modules.identity.domain.repository.RoleRepository;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import com.sa.event_mng.shared.infrastructure.email.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    EmailService emailService;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal
    @Value("${application.security.jwt.secret-key}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${application.security.jwt.expiration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${application.security.jwt.refresh-expiration}")
    protected long REFRESHABLE_DURATION;

    @NonFinal
    @Value("${app.frontend.url}")
    String frontendUrl;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.USER_DISABLED);
        }

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .build();
    }

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    public String register(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }
        if (userRepository.existsByEmailAndEnabledTrue(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        String token = UUID.randomUUID().toString();
        
        String roleName = (request.getRole() != null && request.getRole().equals("ORGANIZER")) ? "ORGANIZER" : "CUSTOMER";
        Role role = roleRepository.findById(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).description(roleName + " role").build()));

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .roles(roles)
                .enabled(false)
                .verificationToken(token)
                .build();

        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
        return "Please check your email to verify your account";
    }

    public String verifyEmail(String token) {
        try {
            User user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

            user.setEnabled(true);
            user.setVerificationToken(null);
            userRepository.save(user);

            return loadHtmlTemplate("templates/verification-success.html")
                    .replace("{{frontendUrl}}", frontendUrl);
        } catch (Exception e) {
            return loadHtmlTemplate("templates/verification-failure.html")
                    .replace("{{frontendUrl}}", frontendUrl);
        }
    }

    private String loadHtmlTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream is = resource.getInputStream()) {
                return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Could not load HTML template: {}", path, e);
            return "Action completed, but template could not be loaded.";
        }
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailAndEnabledTrue(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String otp = String.format("%06d", new Random().nextInt(1000000));
        user.setOtp(otp);
        user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), otp);
        return "OTP sent to your email";
    }

    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByOtp(request.getOtp())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (user.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return "Password reset successful";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ORGANIZER')")
    public String registerStaff(StaffCreateRequest request) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User organizer = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }

        if (userRepository.existsByEmailAndEnabledTrue(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        Role staffRole = roleRepository.findById("STAFF")
                .orElseGet(() -> roleRepository.save(Role.builder().name("STAFF").description("Staff role for check-in").build()));

        String verificationToken = UUID.randomUUID().toString();

        User staff = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .roles(Set.of(staffRole))
                .enabled(false)                    // Chưa kích hoạt
                .verificationToken(verificationToken)
                .organizer(organizer)              // Gắn với Organizer tạo ra
                .build();

        userRepository.save(staff);
        emailService.sendVerificationEmail(staff.getEmail(), verificationToken);
        return "Staff account created. Please ask the staff member to verify their email.";
    }


    public void logout(LogoutRequest request) throws JOSEException, ParseException {
        try {
            var signedToken = verifyToken(request.getToken(), true);

            String jit = signedToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException e) {
            log.info("Token already expired");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) throws JOSEException, ParseException {
        var signedJWT = verifyToken(request.getToken(), true);

        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

        invalidatedTokenRepository.save(invalidatedToken);

        var username = signedJWT.getJWTClaimsSet().getSubject();

        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        var token = generateToken(user);

        return AuthenticationResponse.builder().token(token).build();
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime().toInstant()
                        .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("sa.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> stringJoiner.add("ROLE_" + role.getName()));
        }

        return stringJoiner.toString();
    }
}
