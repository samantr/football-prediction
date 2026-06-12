package com.example.footballprediction.controller;

import com.example.footballprediction.service.ChangePasswordException;
import com.example.footballprediction.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountController {

    private static final String CHANGE_PASSWORD_VIEW = "account/change-password";

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/change-password")
    public String changePassword(Model model) {
        if (!model.containsAttribute("changePasswordForm")) {
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
        }
        return CHANGE_PASSWORD_VIEW;
    }

    @PostMapping("/change-password")
    public String changePassword(
            @ModelAttribute("changePasswordForm") ChangePasswordForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        validate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            return showErrors(form);
        }

        try {
            userService.changePasswordForCurrentUser(
                    authentication.getName(),
                    form.getCurrentPassword(),
                    form.getNewPassword()
            );
        } catch (ChangePasswordException ex) {
            bindingResult.rejectValue(ex.getField(), "changePassword", ex.getMessage());
            return showErrors(form);
        }

        redirectAttributes.addFlashAttribute("success", "Your password has been changed successfully.");
        return "redirect:/account/change-password";
    }

    private void validate(ChangePasswordForm form, BindingResult bindingResult) {
        if (!hasText(form.getCurrentPassword())) {
            bindingResult.rejectValue("currentPassword", "required", "Current password is required.");
        }

        if (!hasText(form.getNewPassword())) {
            bindingResult.rejectValue("newPassword", "required", "New password is required.");
        } else if (form.getNewPassword().trim().length() < 8) {
            bindingResult.rejectValue("newPassword", "size", "New password should be at least 8 characters.");
        }

        if (!hasText(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "required", "Confirm password is required.");
        } else if (hasText(form.getNewPassword()) && !form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "New password and confirm password must match.");
        }
    }

    private String showErrors(ChangePasswordForm form) {
        form.clearPasswords();
        return CHANGE_PASSWORD_VIEW;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
