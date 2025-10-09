package com.litovka.chat.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HomeController is a Spring MVC controller that handles HTTP GET requests
 * and maps them to appropriate view templates.
 * <ul>
 * - The root URL ("/") serves the "chat" page.
 * - The "/login" URL serves the "login" page.
 * </ul>
 * This class uses SLF4J for logging and Spring's @Controller
 * annotation to indicate a web controller.
 */
@Slf4j
@Controller
public class HomeController {

        @GetMapping("/")
        public String index() {
            log.debug("Serving index page request");
            return "chat";
        }

        @GetMapping("/login")
        public String login() {
            log.debug("Serving login page request");
            return "login";
        }

}
