package com.example.cv_rewriter.controller;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {


    @GetMapping("/dashboard")
    public String dashboardPage(Model model, OAuth2AuthenticationToken user) {
        if (user != null) {
            model.addAttribute("name", user.getPrincipal().getAttribute("name"));
            model.addAttribute("email", user.getPrincipal().getAttribute("email"));
        }
        return "dashboard";
    }
}
