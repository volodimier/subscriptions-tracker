package com.subscriptiontracker.service;

import com.subscriptiontracker.constant.DomainConstants;
import com.subscriptiontracker.constant.ErrorMessages;
import com.subscriptiontracker.dto.request.CreateCategoryRequest;
import com.subscriptiontracker.dto.request.UpdateCategoryRequest;
import com.subscriptiontracker.dto.response.CategoryResponse;
import com.subscriptiontracker.entity.Category;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.DuplicateResourceException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling category management operations.
 *
 * <p>Manages the catalog of categories used to organize subscription services.
 * Categories can be system-level (available to all users) or user-specific.</p>
 *
 * <p>Features include:</p>
 * <ul>
 *   <li>Retrieving system categories for dropdown selection</li>
 *   <li>Admin CRUD operations for system categories</li>
 *   <li>Duplicate name detection within the system scope</li>
 *   <li>Preventing deletion of categories in use by services</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see Category
 * @see CategoryRepository
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Retrieves all system categories.
     *
     * <p>System categories are those not owned by any user and are
     * available to all users for organizing their services.</p>
     *
     * @return list of system categories sorted alphabetically by name
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSystemCategories() {
        return categoryRepository.findByUserIsNullOrderByNameAsc().stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new system category (admin only).
     *
     * <p>System categories are shared across all users and can only be
     * created by administrators. The category name must be unique among
     * system categories.</p>
     *
     * @param request the category creation request
     * @return the created category response
     * @throws DuplicateResourceException if a system category with this name already exists
     */
    @Transactional
    public CategoryResponse createSystemCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameAndUserIsNull(request.getName())) {
            throw new DuplicateResourceException(ErrorMessages.CATEGORY_NAME_ALREADY_EXISTS);
        }

        Category category = Category.builder()
                .name(request.getName())
                .user(null)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return CategoryResponse.fromEntity(savedCategory);
    }

    /**
     * Updates an existing system category (admin only).
     *
     * <p>Only the category name can be updated. The new name must be
     * unique among system categories.</p>
     *
     * @param id      the category ID
     * @param request the category update request
     * @return the updated category response
     * @throws ResourceNotFoundException  if the system category is not found
     * @throws DuplicateResourceException if a system category with the new name already exists
     */
    @Transactional
    public CategoryResponse updateSystemCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_CATEGORY, DomainConstants.FIELD_ID, id));

        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByNameAndUserIsNull(request.getName())) {
            throw new DuplicateResourceException(ErrorMessages.CATEGORY_NAME_ALREADY_EXISTS);
        }

        category.setName(request.getName());
        Category savedCategory = categoryRepository.save(category);
        return CategoryResponse.fromEntity(savedCategory);
    }

    /**
     * Deletes a system category (admin only).
     *
     * <p>A system category cannot be deleted if it is currently referenced
     * by any services. Administrators must first reassign or remove all
     * services using this category.</p>
     *
     * @param id the category ID
     * @throws ResourceNotFoundException if the system category is not found
     * @throws BadRequestException       if the category is in use by services
     */
    @Transactional
    public void deleteSystemCategory(Long id) {
        Category category = categoryRepository.findByIdAndUserIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_CATEGORY, DomainConstants.FIELD_ID, id));

        long serviceCount = categoryRepository.countServicesByCategoryId(id);
        if (serviceCount > 0) {
            throw new BadRequestException(
                    String.format(ErrorMessages.CATEGORY_HAS_SERVICES_TEMPLATE, serviceCount)
            );
        }

        categoryRepository.delete(category);
    }
}
