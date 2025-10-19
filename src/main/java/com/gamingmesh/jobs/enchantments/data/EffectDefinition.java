package com.gamingmesh.jobs.enchantments.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines a specific effect and its parameters
 */
public class EffectDefinition {
    private final EnchantmentEffect type;
    private final Map<String, Object> params;

    public EffectDefinition(@NotNull EnchantmentEffect type) {
        this.type = type;
        this.params = new HashMap<>();
    }

    /**
     * Add a parameter to this effect
     */
    public EffectDefinition param(@NotNull String key, @NotNull Object value) {
        params.put(key, value);
        return this;
    }

    /**
     * Get the effect type
     */
    @NotNull
    public EnchantmentEffect getType() {
        return type;
    }

    /**
     * Get all parameters
     */
    @NotNull
    public Map<String, Object> getParams() {
        return new HashMap<>(params);
    }

    /**
     * Get a specific parameter
     */
    @Nullable
    public Object getParam(@NotNull String key) {
        return params.get(key);
    }

    /**
     * Get a parameter as String
     */
    @Nullable
    public String getParamAsString(@NotNull String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Get a parameter as Integer
     */
    public int getParamAsInt(@NotNull String key, int defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Get a parameter as Double
     */
    public double getParamAsDouble(@NotNull String key, double defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Get a parameter as Boolean
     */
    public boolean getParamAsBoolean(@NotNull String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Check if a parameter exists
     */
    public boolean hasParam(@NotNull String key) {
        return params.containsKey(key);
    }

    /**
     * Evaluate a formula parameter
     * Supports variables: {level}, damage, baseDamage, health, maxHealth
     */
    public double evaluateFormula(@NotNull String formula, Map<String, Double> variables) {
        String evaluated = formula;

        // Replace variables
        for (Map.Entry<String, Double> entry : variables.entrySet()) {
            evaluated = evaluated.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }

        // Replace {level} with level from variables
        if (variables.containsKey("level")) {
            evaluated = evaluated.replace("{level}", String.valueOf(variables.get("level")));
        }

        // Simple evaluation (supports +, -, *, /)
        try {
            return evaluateExpression(evaluated);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Simple expression evaluator
     */
    private double evaluateExpression(String expr) {
        // Remove whitespace
        expr = expr.replaceAll("\\s+", "");

        // Handle parentheses recursively
        while (expr.contains("(")) {
            int start = expr.lastIndexOf('(');
            int end = expr.indexOf(')', start);
            if (end == -1) break;

            String sub = expr.substring(start + 1, end);
            double result = evaluateExpression(sub);
            expr = expr.substring(0, start) + result + expr.substring(end + 1);
        }

        // Handle multiplication and division first
        expr = handleOperations(expr, "*/");
        // Then handle addition and subtraction
        expr = handleOperations(expr, "+-");

        return Double.parseDouble(expr);
    }

    private String handleOperations(String expr, String operators) {
        for (char op : operators.toCharArray()) {
            int index;
            while ((index = findOperator(expr, op)) != -1) {
                // Find left operand
                int leftStart = findNumberStart(expr, index - 1);
                double left = Double.parseDouble(expr.substring(leftStart, index));

                // Find right operand
                int rightEnd = findNumberEnd(expr, index + 1);
                double right = Double.parseDouble(expr.substring(index + 1, rightEnd));

                // Calculate result
                double result;
                switch (op) {
                    case '+': result = left + right; break;
                    case '-': result = left - right; break;
                    case '*': result = left * right; break;
                    case '/': result = left / right; break;
                    default: result = 0;
                }

                // Replace in expression
                expr = expr.substring(0, leftStart) + result + expr.substring(rightEnd);
            }
        }
        return expr;
    }

    private int findOperator(String expr, char op) {
        for (int i = 1; i < expr.length(); i++) {
            if (expr.charAt(i) == op) {
                // Make sure it's not a negative sign
                if (op == '-' && i > 0 && Character.isDigit(expr.charAt(i - 1))) {
                    return i;
                } else if (op != '-') {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findNumberStart(String expr, int from) {
        int i = from;
        while (i > 0 && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.' || expr.charAt(i) == '-')) {
            i--;
        }
        return i + 1;
    }

    private int findNumberEnd(String expr, int from) {
        int i = from;
        while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
            i++;
        }
        return i;
    }
}
