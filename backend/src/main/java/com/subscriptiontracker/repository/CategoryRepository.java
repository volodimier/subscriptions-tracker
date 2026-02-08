package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Category entity persistence operations.
 *
 * <p>Provides CRUD operations plus custom queries for managing system
 * and user-specific categories.</p>
 *
 * @author Generated
 * @since 1.0
 * @see Category
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Finds all system categories (those not owned by any user), ordered by name.
     *
     * @return list of system categories sorted alphabetically
     */
    List<Category> findByUserIsNullOrderByNameAsc();

    /**
     * Checks if a system category with the given name already exists.
     *
     * @param name the category name to check
     * @return true if a system category with this name exists
     */
    boolean existsByNameAndUserIsNull(String name);

    /**
     * Finds a system category by its ID.
     *
     * @param id the category ID
     * @return the system category, or empty if not found or not a system category
     */
    Optional<Category> findByIdAndUserIsNull(Long id);

    /**
     * Counts the number of services that reference a specific category.
     *
     * <p>Used to prevent deletion of categories that are currently in use.</p>
     *
     * @param categoryId the category ID
     * @return the number of services using this category
     */
    @Query("SELECT COUNT(s) FROM Service s WHERE s.category.id = :categoryId")
    long countServicesByCategoryId(@Param("categoryId") Long categoryId);
}
