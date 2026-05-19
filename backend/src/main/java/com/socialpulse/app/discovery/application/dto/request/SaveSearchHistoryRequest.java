package com.socialpulse.app.discovery.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 200, message = "Keyword must not exceed 200 characters")
    private String keyword;
}
