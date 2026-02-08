package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.response.CategoryResponse;
import com.subscriptiontracker.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for category operations available to authenticated users.
 *
 * <p>Provides read-only access to system categories for use in dropdown
 * menus and service categorization. Administrative category management
 * is handled by {@link AdminController}.</p>
 *
 * @author Generated
 * @since 1.0
 * @see CategoryService
 * @see AdminController
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category operations for authenticated users")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Retrieves all system categories for dropdown selection.
     *
     * <p>Returns the list of system-level categories that are available to
     * all users for organizing their services.</p>
     *
     * @return list of system categories sorted alphabetically
     */
    @Operation(
            summary = "List system categories",
            description = "Retrieves all system categories available for service categorization."
    )
    @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = categoryService.getSystemCategories();
        return ResponseEntity.ok(categories);
    }
}
