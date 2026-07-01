package com.sa.event_mng.modules.identity.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {
    @NotBlank(message = "USERNAME_REQUIRED")
    @Size(min = 3, max = 50, message = "USERNAME_INVALID")
    private String username;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 6, max = 50, message = "PASSWORD_INVALID")
    private String password;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    private String email;

    @NotBlank(message = "FULLNAME_REQUIRED")
    @Size(max = 50, message = "FULLNAME_TOO_LONG")
    private String fullName;

    @Pattern(regexp = "^$|[0-9]{10,11}$", message = "PHONE_INVALID")
    private String phone;

    @Size(max = 255, message = "ADDRESS_TOO_LONG")
    private String address;

    private String role;
}
