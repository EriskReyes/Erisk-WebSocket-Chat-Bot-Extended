package com.pritzit.benedict.itb2cm321.server.components;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Component for filtering inappropriate words from text messages.
 * Replaces banned words with asterisks (****).
 * Applied to all messages automatically.
 */
@Slf4j
@Component
public class TextFilter {

    private Set<String> bannedWords;

    @PostConstruct
    public void initializeBannedWords() {
        // Default banned words list - can be extended
        bannedWords = new HashSet<>(Arrays.asList(
            "spam", "hack", "virus", "scam", "phishing"
        ));
        log.info("TextFilter initialized with {} banned words", bannedWords.size());
    }

    /**
     * Filters text by replacing banned words with asterisks.
     * This method is applied to ALL messages before they are broadcast.
     * @param text The text to filter
     * @return Filtered text with banned words replaced by ****
     */
    public String filterText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String filteredText = text;

        for (String bannedWord : bannedWords) {
            // Case-insensitive replacement
            filteredText = filteredText.replaceAll("(?i)\\b" + bannedWord + "\\b", "****");
        }

        return filteredText;
    }

    /**
     * Adds a word to the banned words list.
     * @param word The word to ban
     */
    public void addBannedWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            bannedWords.add(word.toLowerCase().trim());
            log.debug("Added banned word: {}", word);
        }
    }

    /**
     * Removes a word from the banned words list.
     * @param word The word to unban
     */
    public void removeBannedWord(String word) {
        if (word != null) {
            bannedWords.remove(word.toLowerCase().trim());
            log.debug("Removed banned word: {}", word);
        }
    }

    /**
     * Gets the current list of banned words.
     * @return Set of banned words
     */
    public Set<String> getBannedWords() {
        return new HashSet<>(bannedWords);
    }
}
