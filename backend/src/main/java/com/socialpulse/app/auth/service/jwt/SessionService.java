package com.socialpulse.app.auth.service.jwt;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    public boolean isSessionValid(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
