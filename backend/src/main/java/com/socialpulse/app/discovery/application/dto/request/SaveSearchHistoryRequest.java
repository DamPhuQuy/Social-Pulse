package com.socialpulse.app.discovery.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveSearchHistoryRequest {
    @NotBlank(message = "Keyword must not be blank")
    private String keyword;
}
