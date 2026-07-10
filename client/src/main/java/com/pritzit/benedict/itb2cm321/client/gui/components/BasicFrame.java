package com.pritzit.benedict.itb2cm321.client.gui.components;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

/**
 * Main GUI frame component for the chat client application.
 * Creates and displays a JFrame window containing the chat panel.
 * Initialized automatically via Spring's PostConstruct mechanism.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BasicFrame {
    private final ChatPanel chatPanel;

    /**
     * Initializes and displays the main application window.
     * Creates a 600x400 JFrame centered on screen with the chat panel.
     */
    @PostConstruct
    public void init(){
        log.info("BasicFrame initialized");

        JFrame frame = new JFrame("ITB2CM321 Chat Client");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.add(chatPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}
