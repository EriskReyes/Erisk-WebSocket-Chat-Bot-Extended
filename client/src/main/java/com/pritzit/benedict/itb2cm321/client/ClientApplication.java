package com.pritzit.benedict.itb2cm321.client;

import com.pritzit.benedict.itb2cm321.client.gui.components.BasicFrame;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main entry point for the WebSocket chat client application.
 * Configures Spring Boot to run in non-headless mode for GUI support.
 * Scans both client and common packages for components.
 * Launches a Swing-based chat interface that connects to the WebSocket server.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.pritzit.benedict.itb2cm321.client", // The server package
                "com.pritzit.benedict.itb2cm321.common"  // The common package, including all services and components
        })
public class ClientApplication {

    /**
     * Main method to start the client application.
     * Sets headless mode to false to enable GUI components.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ClientApplication.class);
        app.setHeadless(false);
        ConfigurableApplicationContext ctx = app.run(args);
    }

}
