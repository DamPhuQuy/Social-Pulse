package com.socialpulse.app.user.application.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AdminAssignRoleRequest {
    @NotEmpty(message = "Roles set cannot be empty")
    private Set<String> roles;
}
