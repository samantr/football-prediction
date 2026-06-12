package com.example.footballprediction.controller;

import com.example.footballprediction.config.SecurityConfig;
import com.example.footballprediction.service.ChangePasswordException;
import com.example.footballprediction.service.CustomUserDetailsService;
import com.example.footballprediction.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void redirectsUnauthenticatedUsersToLogin() throws Exception {
        mockMvc.perform(get("/account/change-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void changesPasswordForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/account/change-password")
                        .with(csrf())
                        .param("currentPassword", "old-password")
                        .param("newPassword", "new-password")
                        .param("confirmPassword", "new-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/account/change-password"))
                .andExpect(flash().attribute("success", "Your password has been changed successfully."));

        verify(userService).changePasswordForCurrentUser("user@example.com", "old-password", "new-password");
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void rejectsWrongCurrentPasswordOnSamePage() throws Exception {
        doThrow(new ChangePasswordException("currentPassword", "Current password is incorrect."))
                .when(userService)
                .changePasswordForCurrentUser("user@example.com", "wrong-password", "new-password");

        mockMvc.perform(post("/account/change-password")
                        .with(csrf())
                        .param("currentPassword", "wrong-password")
                        .param("newPassword", "new-password")
                        .param("confirmPassword", "new-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/change-password"))
                .andExpect(model().attributeHasFieldErrors("changePasswordForm", "currentPassword"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void rejectsMismatchedNewPasswordsOnSamePage() throws Exception {
        mockMvc.perform(post("/account/change-password")
                        .with(csrf())
                        .param("currentPassword", "old-password")
                        .param("newPassword", "new-password")
                        .param("confirmPassword", "different-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/change-password"))
                .andExpect(model().attributeHasFieldErrors("changePasswordForm", "confirmPassword"));

        verifyNoInteractions(userService);
    }
}
