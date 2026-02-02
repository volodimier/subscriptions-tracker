package com.subscriptiontracker.security;

import com.subscriptiontracker.entity.Role;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserDetailsServiceImpl}.
 *
 * <p>Tests the user details service including role-based authority
 * assignment for authentication.</p>
 *
 * @author Generated
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User regularUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        regularUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("hashedPassword123")
                .baseCurrencyCode("USD")
                .role(Role.USER)
                .build();

        adminUser = User.builder()
                .id(2L)
                .email("admin@example.com")
                .passwordHash("hashedPassword456")
                .baseCurrencyCode("EUR")
                .role(Role.ADMIN)
                .build();
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("should return user details with ROLE_USER authority for regular user")
        void shouldReturnUserDetailsWithRoleUserAuthorityForRegularUser() {
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(regularUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("user@example.com");

            assertNotNull(userDetails);
            assertEquals("user@example.com", userDetails.getUsername());
            assertEquals("hashedPassword123", userDetails.getPassword());
            assertTrue(userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(auth -> auth.equals("ROLE_USER")));
        }

        @Test
        @DisplayName("should return user details with ROLE_ADMIN authority for admin user")
        void shouldReturnUserDetailsWithRoleAdminAuthorityForAdminUser() {
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("admin@example.com");

            assertNotNull(userDetails);
            assertEquals("admin@example.com", userDetails.getUsername());
            assertEquals("hashedPassword456", userDetails.getPassword());
            assertTrue(userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(auth -> auth.equals("ROLE_ADMIN")));
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user not found")
        void shouldThrowUsernameNotFoundExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            UsernameNotFoundException exception = assertThrows(
                    UsernameNotFoundException.class,
                    () -> userDetailsService.loadUserByUsername("unknown@example.com")
            );

            assertTrue(exception.getMessage().contains("unknown@example.com"));
        }

        @Test
        @DisplayName("should have exactly one authority")
        void shouldHaveExactlyOneAuthority() {
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(regularUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("user@example.com");

            assertEquals(1, userDetails.getAuthorities().size());
        }

        @Test
        @DisplayName("should use correct authority format with ROLE_ prefix")
        void shouldUseCorrectAuthorityFormatWithRolePrefix() {
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("admin@example.com");

            String authority = userDetails.getAuthorities().iterator().next().getAuthority();
            assertTrue(authority.startsWith("ROLE_"));
            assertEquals("ROLE_ADMIN", authority);
        }
    }
}
