package com.sa.event_mng.modules.identity.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {

    @NotBlank(message = "USERNAME_REQUIRED")
    private String username;

    @NotBlank(message = "PASSWORD_REQUIRED")
    private String password;
}
