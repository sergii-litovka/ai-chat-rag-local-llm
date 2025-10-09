package com.litovka.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
@SpringBootApplication
public class AiChatApplication {

	public static void main(String[] args) {
		log.info("Starting AI Chat Application...");
		try {
			ConfigurableApplicationContext context = SpringApplication.run(AiChatApplication.class, args);
			log.info("AI Chat Application started successfully");
		} catch (Exception e) {
			log.error("Failed to start AI Chat Application", e);
			throw e;
		}
	}

}
