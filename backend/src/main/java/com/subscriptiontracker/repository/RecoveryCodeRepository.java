package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.RecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing {@link RecoveryCode} entities.
 *
 * <p>Provides database operations for recovery codes used as backup
 * authentication method for two-factor authentication.</p>
 *
 * @author Generated
 * @since 1.0
 * @see RecoveryCode
 */
@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {

    /**
     * Finds all recovery codes for a specific user.
     *
     * @param userId the user's ID
     * @return list of recovery codes belonging to the user
     */
    List<RecoveryCode> findByUserId(Long userId);

    /**
     * Finds all unused (available) recovery codes for a specific user.
     *
     * @param userId the user's ID
     * @return list of unused recovery codes belonging to the user
     */
    @Query("SELECT rc FROM RecoveryCode rc WHERE rc.user.id = :userId AND rc.usedAt IS NULL")
    List<RecoveryCode> findUnusedByUserId(@Param("userId") Long userId);

    /**
     * Counts unused recovery codes for a specific user.
     *
     * @param userId the user's ID
     * @return count of unused recovery codes
     */
    @Query("SELECT COUNT(rc) FROM RecoveryCode rc WHERE rc.user.id = :userId AND rc.usedAt IS NULL")
    long countUnusedByUserId(@Param("userId") Long userId);

    /**
     * Deletes all recovery codes for a specific user.
     *
     * <p>Used when regenerating recovery codes or disabling 2FA.</p>
     *
     * @param userId the user's ID
     */
    @Modifying
    @Query("DELETE FROM RecoveryCode rc WHERE rc.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
