package com.osigie.erecall.security;

import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final UserRepository userRepository;

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof String email) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        }
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + userDetails.getUsername()));
        }
        throw new IllegalStateException("No authenticated user found");
    }
}
