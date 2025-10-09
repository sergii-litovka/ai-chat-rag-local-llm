package com.litovka.chat.conf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration class for setting up application security.
 *
 * This class configures Spring Security to handle authentication and authorization.
 * It defines beans for setting security filter chains, user details service,
 * and password encoding mechanism.
 *
 * Key Features:
 * - Configures allowed access to static resources such as CSS, JavaScript, and images.
 * - Secures all other requests by requiring authentication.
 * - Enables form-based login with a custom login page and a defined default success URL.
 * - Configures logout functionality with a custom logout success URL.
 * - Provides an in-memory user details service with a predefined username and password.
 * - Utilizes the BCrypt password encoder for password security.
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/", true)
            )
            .logout(logout -> logout
                .permitAll()
                .logoutSuccessUrl("/login")
            );
        
        log.info("Security filter chain configured successfully");
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        log.info("Creating in-memory user details service");
        UserDetails user = User.builder()
            .username("litovka")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();

        log.info("User details service created with user: {}", user.getUsername());
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Creating BCrypt password encoder");
        return new BCryptPasswordEncoder();
    }
}