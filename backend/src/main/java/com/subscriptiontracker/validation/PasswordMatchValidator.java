package com.subscriptiontracker.validation;

import com.subscriptiontracker.dto.request.ChangePasswordRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, ChangePasswordRequest> {

    @Override
    public boolean isValid(ChangePasswordRequest request, ConstraintValidatorContext context) {
        if (request.getNewPassword() == null || request.getConfirmNewPassword() == null) {
            return true; // Let @NotBlank handle null validation
        }
        return request.getNewPassword().equals(request.getConfirmNewPassword());
    }
}
