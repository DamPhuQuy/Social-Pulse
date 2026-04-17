package com.socialpulse.app.auth.security.user;

import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.enums.UserRole;
import com.socialpulse.app.user.enums.UserStatus;
import com.socialpulse.app.user.enums.VerificationStatus;

import lombok.Builder;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Builder
public class CustomUserDetails implements UserDetails {
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        UserRole role = user.getRole();

        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        role.getPermissions().forEach((permission -> authorities.add(new SimpleGrantedAuthority(permission.name()))));

        return authorities;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPasswordHash();
    }

    // QUAN TRọNG: trả email (không phải username) vì project xác thực bằng email.
    // Spring Security dùng giá trị này làm "principal" và JJWT lưu vào JWT subject.
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    // Expose User entity để AuthService khỏi tạo JWT claims (userId, role)
    public User getUser() {
        return user;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE && user.getVerification() == VerificationStatus.VERIFIED;
    }
}
