package com.subscriptiontracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a recovery code for two-factor authentication backup.
 *
 * <p>Recovery codes are single-use backup codes that allow users to authenticate
 * when they don't have access to their TOTP device. Each user is typically given
 * 10 recovery codes when enabling 2FA.</p>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Recovery codes are BCrypt-hashed before storage</li>
 *   <li>Each code can only be used once (tracked via usedAt)</li>
 *   <li>Codes are deleted and regenerated when user requests new codes</li>
 *   <li>Codes are deleted when 2FA is disabled</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see User
 */
@Entity
@Table(name = "recovery_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryCode {

    /**
     * Unique identifier for the recovery code.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user associated with this recovery code.
     * Uses lazy loading to avoid fetching user data when not needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The BCrypt-hashed recovery code.
     * The original plaintext code is shown only once during generation.
     */
    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    /**
     * Timestamp when the recovery code was created.
     * Automatically set on entity creation.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the recovery code was used.
     * Null if the code has not been used yet.
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * Checks if this recovery code has been used.
     *
     * @return true if the code has been used, false otherwise
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Checks if this recovery code is available for use.
     *
     * @return true if the code has not been used, false otherwise
     */
    public boolean isAvailable() {
        return usedAt == null;
    }
}
