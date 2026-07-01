package com.sa.event_mng.modules.identity.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffCreateRequest {
    @Size(min = 3, max = 50, message = "USERNAME_INVALID")
    @NotBlank(message = "USERNAME_REQUIRED")
    private String username;

    @Email(message = "EMAIL_INVALID")
    @NotBlank(message = "EMAIL_REQUIRED")
    private String email;

    @Size(min = 6, max = 50, message = "PASSWORD_INVALID")
    @NotBlank(message = "PASSWORD_REQUIRED")
    private String password;

    @NotBlank(message = "FULLNAME_REQUIRED")
    private String fullName;
}
