package com.socialpulse.app.admin.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BanUserRequest {
    @NotNull
    private Boolean ban; // true = ban, false = unban
}
