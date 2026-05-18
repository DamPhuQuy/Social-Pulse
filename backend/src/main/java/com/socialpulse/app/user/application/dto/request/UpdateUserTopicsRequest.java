package com.socialpulse.app.user.application.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserTopicsRequest {
    @NotNull(message = "Topic IDs must not be null")
    @Size(max = 20, message = "Cannot select more than 20 topics")
    private List<Long> topicIds;
}
