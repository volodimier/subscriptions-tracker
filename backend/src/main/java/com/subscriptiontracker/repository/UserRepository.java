package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity persistence operations.
 *
 * <p>Provides standard CRUD operations via JpaRepository plus custom
 * query methods for user lookup by email.</p>
 *
 * @author Generated
 * @since 1.0
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email address to search for
     * @return optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email the email address to check
     * @return true if a user exists with this email
     */
    boolean existsByEmail(String email);

    /**
     * Finds unverified users whose accounts were created before the cutoff date.
     *
     * <p>This is used by the cleanup scheduler to identify accounts that have
     * exceeded the grace period without verifying their email address.</p>
     *
     * @param cutoffDate the date before which unverified accounts should be found
     * @return list of unverified users created before the cutoff date
     */
    @Query("SELECT u FROM User u " +
           "WHERE u.emailVerified = false " +
           "AND u.createdAt < :cutoffDate")
    List<User> findUnverifiedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Updates the createdAt timestamp for a user.
     *
     * <p>This method uses a native query to bypass JPA's updatable=false constraint
     * on the createdAt column. It is primarily intended for testing purposes.</p>
     *
     * @param userId    the user's ID
     * @param createdAt the new createdAt timestamp
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET created_at = :createdAt WHERE id = :userId", nativeQuery = true)
    void updateCreatedAt(@Param("userId") Long userId, @Param("createdAt") LocalDateTime createdAt);
}
