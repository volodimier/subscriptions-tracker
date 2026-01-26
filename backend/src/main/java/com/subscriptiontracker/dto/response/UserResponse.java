package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String baseCurrencyCode;
    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .baseCurrencyCode(user.getBaseCurrencyCode())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
