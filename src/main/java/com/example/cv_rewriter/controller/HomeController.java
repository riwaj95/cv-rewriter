package com.example.cv_rewriter.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication){
        if(authentication != null && authentication.isAuthenticated()){
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @RequestMapping("/user")
    @ResponseBody
    public Principal user(Principal user){
        return user;
    }
}
