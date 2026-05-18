package com.socialpulse.app.admin.application.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChangeUserRoleRequest {
    @NotEmpty
    private Set<String> roles; // e.g. ["USER"], ["ADMIN"]
}
