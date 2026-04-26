package com.socialpulse.app.security.user;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.socialpulse.app.user.domain.enums.UserStatus;
import com.socialpulse.app.user.domain.enums.VerificationStatus;
import com.socialpulse.app.user.domain.model.User;

import lombok.Builder;

@Builder
public class CustomUserDetails implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_";

    private final User user;
    private final Collection<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        this.authorities = Optional.ofNullable(user.getRoles())
                            .orElse(Set.of())
                            .stream()
                            .flatMap(role -> Stream.concat(
                                    Stream.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.getName())),
                                    Optional.ofNullable(role.getPermissions())
                                            .orElse(Set.of())
                                            .stream()
                                            .map(permission -> new SimpleGrantedAuthority(permission.getName())
                            )))
                            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPasswordHash();
    }

    // Spring Security dùng giá trị này làm principal và JJWT lưu vào JWT subject.
    @Override
    @NullMarked
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    @NullMarked
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isLocked();
    }

    @Override
    @NullMarked
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE && user.getVerification() == VerificationStatus.VERIFIED;
    }
}
