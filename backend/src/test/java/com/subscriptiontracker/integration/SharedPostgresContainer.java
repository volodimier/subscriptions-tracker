package com.subscriptiontracker.integration;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * Shared PostgreSQL container singleton for all integration tests.
 *
 * <p>This class provides a single PostgreSQL container instance that is
 * started once and shared across all integration test classes. Using a
 * singleton pattern instead of @Container annotation provides better
 * compatibility with CI environments like GitHub Actions.</p>
 *
 * <p>The container is started lazily on first access and remains running
 * for the duration of the test suite.</p>
 *
 * @author Generated
 * @since 1.0
 */
public final class SharedPostgresContainer {

    private static final PostgreSQLContainer<?> INSTANCE;

    static {
        INSTANCE = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("subscription_tracker_test")
                .withUsername("test")
                .withPassword("test")
                .waitingFor(
                        Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 1)
                                .withStartupTimeout(Duration.ofSeconds(60))
                );
        INSTANCE.start();
    }

    private SharedPostgresContainer() {
        // Prevent instantiation
    }

    /**
     * Returns the shared PostgreSQL container instance.
     *
     * @return the PostgreSQL container
     */
    public static PostgreSQLContainer<?> getInstance() {
        return INSTANCE;
    }
}
