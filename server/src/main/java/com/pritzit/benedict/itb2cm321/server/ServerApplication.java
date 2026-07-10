package com.pritzit.benedict.itb2cm321.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main entry point for the WebSocket chat server application.
 * Configures Spring Boot to scan both server and common packages for components.
 * Provides WebSocket message handling, broadcasting, and command processing capabilities.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.pritzit.benedict.itb2cm321.server", // The server package
                "com.pritzit.benedict.itb2cm321.common"  // The common package, including all services and components
        })
public class ServerApplication {

    /**
     * Main method to start the server application.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

}
