package com.subscriptiontracker.security;

import com.subscriptiontracker.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT authentication filter that processes Bearer tokens in requests.
 *
 * <p>This filter extracts and validates JWT tokens from the Authorization header.
 * It supports two types of tokens:</p>
 * <ul>
 *   <li><strong>Full tokens</strong>: Standard JWT for fully authenticated users</li>
 *   <li><strong>Partial tokens</strong>: Limited JWT for users with 2FA pending,
 *       which grants only the TWO_FACTOR_PENDING authority</li>
 * </ul>
 *
 * <p>Partial tokens can only be used to access 2FA verification endpoints.</p>
 *
 * @author Generated
 * @since 1.0
 * @see JwtService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Authority granted to users with pending 2FA verification. */
    public static final String TWO_FACTOR_PENDING_AUTHORITY = "TWO_FACTOR_PENDING";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Check if this is a partial token (2FA pending)
                if (jwtService.isTwoFactorPending(jwt)) {
                    handlePartialToken(request, jwt, userEmail);
                } else {
                    handleFullToken(request, jwt, userEmail);
                }
            }
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getClass().getSimpleName());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Handles a full JWT token for fully authenticated users.
     *
     * @param request   the HTTP request
     * @param jwt       the JWT token
     * @param userEmail the user's email from the token
     */
    private void handleFullToken(HttpServletRequest request, String jwt, String userEmail) {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

        if (jwtService.isTokenValid(jwt, userDetails)) {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }

    /**
     * Handles a partial JWT token for users with pending 2FA verification.
     *
     * <p>Partial tokens grant only the TWO_FACTOR_PENDING authority, which
     * allows access to 2FA verification endpoints only.</p>
     *
     * @param request   the HTTP request
     * @param jwt       the partial JWT token
     * @param userEmail the user's email from the token
     */
    private void handlePartialToken(HttpServletRequest request, String jwt, String userEmail) {
        // For partial tokens, we create a limited authentication with only TWO_FACTOR_PENDING authority
        Long userId = jwtService.extractUserId(jwt);

        if (userId != null) {
            // Create a principal that contains the user ID for later use
            TwoFactorPendingPrincipal principal = new TwoFactorPendingPrincipal(userId, userEmail, jwt);

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(TWO_FACTOR_PENDING_AUTHORITY))
            );
            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("Partial token authenticated for 2FA pending user: {}", userEmail);
        }
    }

    /**
     * Principal class for users with pending 2FA verification.
     *
     * <p>Contains the user ID and email needed to complete 2FA verification.</p>
     */
    public record TwoFactorPendingPrincipal(Long userId, String email, String partialToken) {
    }
}
