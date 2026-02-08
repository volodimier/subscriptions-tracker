package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for SubscriptionHistory entity persistence operations.
 *
 * <p>Provides CRUD operations and a query for retrieving the full
 * change timeline of a subscription, ordered by most recent first.</p>
 *
 * @author Generated
 * @since 1.0
 * @see SubscriptionHistory
 */
@Repository
public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {

    /**
     * Finds all history entries for a subscription, ordered by changed_at descending.
     *
     * <p>Returns the complete change timeline for a subscription with the
     * most recent changes first.</p>
     *
     * @param subscriptionId the ID of the subscription
     * @return list of history entries ordered by changedAt descending
     */
    List<SubscriptionHistory> findBySubscriptionIdOrderByChangedAtDesc(Long subscriptionId);
}
