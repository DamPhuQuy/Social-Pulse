package com.socialpulse.app.user.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.socialpulse.app.user.domain.enums.UserGender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
	private Long id;
	private String displayName;
	private String bio;
	private LocalDate dob;
	private UserGender gender;
	private String avatarUrl;
	private String avatarPublicId;
	private String coverImageUrl;
	private String coverImagePublicId;
	private LocalDateTime updatedAt;
}
