package com.example.cv_rewriter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig{

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/").permitAll();
                    auth.anyRequest().permitAll();
                })
                .formLogin(form -> form.defaultSuccessUrl("/dashboard",true))
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/dashboard", true)  // Ensure this is set
                );

        return http.build();
    }

}
