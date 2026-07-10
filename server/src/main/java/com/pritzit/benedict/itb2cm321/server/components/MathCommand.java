package com.pritzit.benedict.itb2cm321.server.components;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Command for handling mathematical operations.
 * Supports addition, subtraction, multiplication, and division.
 * Evaluates expressions from left to right without operator precedence.
 */
@Slf4j
@Component
public class MathCommand {

    /**
     * Executes the math command by evaluating a mathematical expression.
     * @param expression The mathematical expression to evaluate (e.g., "2+3*4")
     * @return The result as a string, or error message if invalid
     */
    public String execute(String expression) {
        try {
            // Remove all spaces
            String cleanExpression = expression.replaceAll("\\s+", "");

            // Validate expression contains only valid characters
            if (!cleanExpression.matches("[0-9+\\-*/.,]+")) {
                return "Error: Invalid characters in expression";
            }

            // Replace comma with dot for decimal handling
            cleanExpression = cleanExpression.replace(",", ".");

            double result = evaluateExpression(cleanExpression);

            // Return integer if result is whole number
            if (result == (long) result) {
                return String.valueOf((long) result);
            } else {
                return String.valueOf(result);
            }

        } catch (Exception e) {
            log.debug("Error calculating expression: {}", expression, e);
            return "Error: Invalid mathematical expression";
        }
    }

    /**
     * Evaluates mathematical expression from left to right (no operator precedence).
     * @param expression Clean expression without spaces
     * @return The calculated result
     */
    private double evaluateExpression(String expression) {
        // Split by operators while keeping them
        String[] tokens = expression.split("(?=[+\\-*/])|(?<=[+\\-*/])");

        if (tokens.length == 0) {
            throw new IllegalArgumentException("Empty expression");
        }

        double result = Double.parseDouble(tokens[0]);

        for (int i = 1; i < tokens.length; i += 2) {
            if (i + 1 >= tokens.length) {
                throw new IllegalArgumentException("Invalid expression format");
            }

            String operator = tokens[i];
            double operand = Double.parseDouble(tokens[i + 1]);

            switch (operator) {
                case "+" -> result += operand;
                case "-" -> result -= operand;
                case "*" -> result *= operand;
                case "/" -> {
                    if (operand == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    result /= operand;
                }
                default -> throw new IllegalArgumentException("Unknown operator: " + operator);
            }
        }

        return result;
    }
}
