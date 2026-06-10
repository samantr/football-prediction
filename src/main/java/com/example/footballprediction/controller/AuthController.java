package com.example.footballprediction.controller;

import com.example.footballprediction.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        if (!model.containsAttribute("email")) {
            model.addAttribute("email", "");
        }
        if (!model.containsAttribute("displayName")) {
            model.addAttribute("displayName", "");
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(
            @RequestParam String email,
            @RequestParam String displayName,
            @RequestParam String password,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.signUp(email, displayName, password);
            redirectAttributes.addFlashAttribute("success", "Account created. Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("displayName", displayName);
            return "redirect:/signup";
        }
    }
}
