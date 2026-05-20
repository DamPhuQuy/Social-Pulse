package com.socialpulse.app.admin.application.dto;

import java.util.Set;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RbacRoleResponse {
    private String name;
    private String description;
    private Set<String> permissions;
}
