package com.kh.serviceplatform.common.security;

import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.features.auth.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ForbiddenException("User is not authenticated");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw new ForbiddenException("Invalid user authentication principal");
        }
    }

    public static UserRole getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("User is not authenticated");
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String roleName = authority.getAuthority();
            if (roleName.startsWith("ROLE_")) {
                try {
                    return UserRole.valueOf(roleName.substring(5));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        throw new ForbiddenException("User role not recognized");
    }
}
