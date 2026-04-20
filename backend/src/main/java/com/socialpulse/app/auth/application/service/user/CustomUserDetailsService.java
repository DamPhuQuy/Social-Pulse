package com.socialpulse.app.auth.application.service.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.user.application.port.out.UserRepositoryPort;
import com.socialpulse.app.user.domain.model.User;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepositoryPort userRepository;

    public CustomUserDetailsService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return CustomUserDetails.builder()
                .user(user)
                .build();
    }
}
