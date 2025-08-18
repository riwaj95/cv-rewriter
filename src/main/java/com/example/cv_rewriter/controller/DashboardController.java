package com.example.cv_rewriter.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboardPage(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof OAuth2User) {
                OAuth2User oauthUser = (OAuth2User) principal;
                model.addAttribute("name", oauthUser.getAttribute("name"));
                model.addAttribute("email", oauthUser.getAttribute("email"));
            } else {
                String username = authentication.getName();
                model.addAttribute("name", username);
                model.addAttribute("email", "No email available");
            }
        }
        return "dashboard";
    }
}
