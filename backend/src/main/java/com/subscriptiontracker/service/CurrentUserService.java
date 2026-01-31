package com.subscriptiontracker.service;

import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving the current authenticated user from the security context.
 * Centralizes user lookup logic that was previously duplicated across controllers.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    /**
     * Retrieves the User entity for the currently authenticated user.
     *
     * @param userDetails the Spring Security UserDetails from @AuthenticationPrincipal
     * @return the User entity
     * @throws ResourceNotFoundException if the user is not found
     */
    public User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()));
    }

    /**
     * Retrieves the user ID for the currently authenticated user.
     *
     * @param userDetails the Spring Security UserDetails from @AuthenticationPrincipal
     * @return the user's ID
     * @throws ResourceNotFoundException if the user is not found
     */
    public Long getCurrentUserId(UserDetails userDetails) {
        return getCurrentUser(userDetails).getId();
    }
}
