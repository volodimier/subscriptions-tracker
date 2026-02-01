package com.subscriptiontracker.integration;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to conditionally enable tests only when Docker is available.
 *
 * <p>Apply this annotation to test classes or methods that require Docker
 * (e.g., Testcontainers-based integration tests). When Docker is not available,
 * the annotated test will be skipped with an informative message.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @EnabledIfDockerAvailable
 * class MyIntegrationTest extends BaseIntegrationTest {
 *     // tests that require Docker
 * }
 * }
 * </pre>
 *
 * @author Generated
 * @since 1.0
 * @see DockerAvailableCondition
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DockerAvailableCondition.class)
public @interface EnabledIfDockerAvailable {
}
