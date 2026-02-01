package com.subscriptiontracker.integration;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit 5 execution condition that disables tests when Docker is not available.
 *
 * <p>This condition checks whether Docker is running and accessible on the system.
 * If Docker is not available, the test class or method is disabled with an
 * informative message.</p>
 *
 * <p>Use with {@link EnabledIfDockerAvailable} annotation.</p>
 *
 * @author Generated
 * @since 1.0
 * @see EnabledIfDockerAvailable
 */
public class DockerAvailableCondition implements ExecutionCondition {

    private static final String DISABLED_REASON = "Docker is not available. " +
            "Integration tests require Docker to run Testcontainers.";
    private static final String ENABLED_REASON = "Docker is available.";

    /**
     * Cached result of Docker availability check to avoid repeated checks.
     */
    private static volatile Boolean dockerAvailable = null;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (isDockerAvailable()) {
            return ConditionEvaluationResult.enabled(ENABLED_REASON);
        }
        return ConditionEvaluationResult.disabled(DISABLED_REASON);
    }

    /**
     * Checks if Docker is available on the current system.
     *
     * <p>The result is cached to avoid repeated expensive checks.</p>
     *
     * @return true if Docker is available, false otherwise
     */
    private static boolean isDockerAvailable() {
        if (dockerAvailable == null) {
            synchronized (DockerAvailableCondition.class) {
                if (dockerAvailable == null) {
                    try {
                        dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
                    } catch (Exception e) {
                        dockerAvailable = false;
                    }
                }
            }
        }
        return dockerAvailable;
    }
}
