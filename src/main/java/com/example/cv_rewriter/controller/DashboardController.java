package com.example.cv_rewriter.controller;

import com.example.cv_rewriter.service.CvProcessRecordService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final CvProcessRecordService cvProcessRecordService;

    public DashboardController(CvProcessRecordService cvProcessRecordService) {
        this.cvProcessRecordService = cvProcessRecordService;
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof OAuth2User) {
                OAuth2User oauthUser = (OAuth2User) principal;
                String email = attributeAsString(oauthUser, "email");
                model.addAttribute("name", attributeAsString(oauthUser, "name"));
                model.addAttribute("email", email);
                model.addAttribute("history", cvProcessRecordService.fetchRecentHistory(email));
            } else {
                String username = authentication.getName();
                model.addAttribute("name", username);
                model.addAttribute("email", "No email available");
                model.addAttribute("history", List.of());
            }
        } else {
            model.addAttribute("history", List.of());
        }
        return "dashboard";
    }

    private String attributeAsString(OAuth2User user, String attributeName) {
        if (user == null || attributeName == null) {
            return null;
        }
        Object value = user.getAttribute(attributeName);
        return value != null ? value.toString() : null;
    }
}
