package com.example.cv_rewriter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class HomeController {

    @GetMapping("/")
    public String hello(){
        return "Hello";
    }

    @RequestMapping("/user")
    public Principal user(Principal user){
        return user;
    }
}
