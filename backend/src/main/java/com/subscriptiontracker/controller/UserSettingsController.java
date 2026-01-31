package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.ChangePasswordRequest;
import com.subscriptiontracker.dto.request.DeleteAccountRequest;
import com.subscriptiontracker.dto.request.UpdateUserSettingsRequest;
import com.subscriptiontracker.dto.response.UserSettingsResponse;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    @GetMapping("/settings")
    public ResponseEntity<UserSettingsResponse> getSettings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        UserSettingsResponse response = userService.getSettings(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/settings")
    public ResponseEntity<UserSettingsResponse> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUserSettingsRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        UserSettingsResponse response = userService.updateSettings(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeleteAccountRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        userService.deleteAccount(userId, request);
        return ResponseEntity.noContent().build();
    }
}
