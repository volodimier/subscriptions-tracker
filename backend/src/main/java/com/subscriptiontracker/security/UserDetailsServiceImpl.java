package com.subscriptiontracker.security;

import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Implementation of Spring Security's {@link UserDetailsService} for loading user details.
 *
 * <p>Retrieves user information from the database and converts it to Spring Security's
 * {@link UserDetails} format for authentication and authorization. The user's role
 * is dynamically loaded from the database and converted to a Spring Security authority.</p>
 *
 * @author Generated
 * @since 1.0
 * @see com.subscriptiontracker.entity.User
 * @see com.subscriptiontracker.entity.Role
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads user details by email address for authentication.
     *
     * <p>Retrieves the user from the database and converts their role to a
     * Spring Security authority in the format "ROLE_{roleName}".</p>
     *
     * @param email the user's email address (used as username)
     * @return the user details for Spring Security authentication
     * @throws UsernameNotFoundException if no user exists with the given email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.subscriptiontracker.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String authority = "ROLE_" + user.getRole().name();
        return new User(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
    }
}
