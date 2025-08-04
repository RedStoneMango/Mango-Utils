/*
 * Copyright (c) 2025 RedStoneMango
 *
 * This file is licensed under the MIT License.
 * You may use, copy, modify, and distribute this file under the terms of the MIT License.
 * See the LICENSE file or https://opensource.org/licenses/MIT for full text.
 */

package io.github.redstonemango.mangoutils;

import java.util.Locale;

/**
 * Utility class for converting texts between different {@linkplain NamingConvention naming conventions}.
 *
 * @author RedStoneMango
 */
public class NameConverter {

    /**
     * Converts a string from one naming convention to another using the root locale.
     *
     * @param input The string to convert
     * @param inputConvention The convention, the input string is written in. This may be {@link NamingConvention#MIXED_CASE_TYPES MIXED_CASE_TYPES}
     * @param outputConvention The convention to convert to.
     * @return The converted string.
     * @throws IllegalArgumentException If one of the specified conventions is null or invalid in the current context.
     * @see #convert(String, NamingConvention, NamingConvention, Locale)
     */
    public static String convert(String input, NamingConvention inputConvention, NamingConvention outputConvention) {
        return convert(input, inputConvention, outputConvention, Locale.ROOT);
    }
    /**
     * Converts a string from one naming convention to another using the root locale.
     *
     * @param input The string to convert
     * @param inputConvention The convention, the input string is written in. This may be {@link NamingConvention#MIXED_CASE_TYPES MIXED_CASE_TYPES}
     * @param outputConvention The convention to convert to.
     * @param locale The locale to use.
     * @return The converted string.
     * @throws IllegalArgumentException If one of the specified conventions is null or invalid in the current context.
     * @see #convert(String, NamingConvention, NamingConvention)
     */
    public static String convert(String input, NamingConvention inputConvention, NamingConvention outputConvention, Locale locale) {
        if (inputConvention == outputConvention) return input;
        String plain = inputConvention == NamingConvention.MIXED_CASE_TYPES ? mixedToPlain(input, locale) : customToPlain(input, inputConvention, locale);
        return plainToCustom(plain, outputConvention, locale);
    }

    /**
     * Converts a text written in a mix of multiple conventions to a plain text by specifically calling {@link #customToPlain(String, NamingConvention, Locale)}.
     *
     * @param mixedText The text written in mixed naming conventions.
     * @param locale The locale to use.
     * @return The plain text.
     */
    private static String mixedToPlain(String mixedText, Locale locale) {
        if (mixedText.chars().anyMatch(Character::isUpperCase)) mixedText = customToPlain(mixedText, NamingConvention.PASCAL_CASE, locale);
        if (mixedText.contains("_")) mixedText = customToPlain(mixedText, NamingConvention.SNAKE_CASE, locale);
        if (mixedText.contains("-")) mixedText = customToPlain(mixedText, NamingConvention.KEBAB_CASE, locale);
        return mixedText;
    }

    /**
     * Converts a text written in a specific naming convention into plain text.
     *
     * @param customText The text to convert.
     * @param convention The naming convention the input text is written in. This may <b><u>not</u></b> be {@link NamingConvention#MIXED_CASE_TYPES MIXED_CASE_TYPES}.
     * @param locale The locale to use.
     * @return The plain text.
     */
    private static String customToPlain(String customText, NamingConvention convention, Locale locale) {
        switch (convention) {
            case CAMEL_CASE, PASCAL_CASE -> {
                String[] parts = customText.split("(?=[A-Z])");
                StringBuilder builder = new StringBuilder();
                for (String part : parts) {
                    builder.append(builder.isEmpty() ? "" : " ").append(part.toLowerCase(locale));
                }
                return builder.toString().replaceAll(" {2,}", " ");
            }
            case SNAKE_CASE -> {
                String[] parts = customText.split("_");
                StringBuilder builder = new StringBuilder();
                for (String part : parts) {
                    builder.append(builder.isEmpty() ? "" : " ").append(part);
                }
                return builder.toString().replaceAll(" {2,}", " ");
            }
            case KEBAB_CASE -> {
                String[] parts = customText.split("-");
                StringBuilder builder = new StringBuilder();
                for (String part : parts) {
                    builder.append(builder.isEmpty() ? "" : " ").append(part);
                }
                return builder.toString().replaceAll(" {2,}", " ");
            }
            case PLAIN_TEXT -> {
                return customText;
            }
            case null, default -> throw new IllegalArgumentException("Invalid naming convention: " + convention);
        }
    }
    /**
     * Converts plain text into a specific naming convention.
     *
     * @param plainText The text to convert.
     * @param convention The naming convention to convert to. This may <b><u>not</u></b> be {@link NamingConvention#MIXED_CASE_TYPES MIXED_CASE_TYPES}.
     * @param locale The locale to use.
     * @return The converted text.
     */
    private static String plainToCustom(String plainText, NamingConvention convention, Locale locale) {
        switch (convention) {
            case CAMEL_CASE -> {
                String[] parts = plainText.split(" ");
                StringBuilder builder = new StringBuilder();
                for (String part : parts) {
                    String start = part.isEmpty() ? "" : part.substring(0, 1);
                    String rest = part.length() <= 1 ? "" : part.substring(1);
                    builder.append(builder.isEmpty() ? start : start.toUpperCase(locale)).append(rest.toLowerCase(locale));
                }
                return builder.toString();
            }
            case PASCAL_CASE -> {
                String[] parts = plainText.split(" ");
                StringBuilder builder = new StringBuilder();
                for (String part : parts) {
                    String start = part.isEmpty() ? "" : part.substring(0, 1);
                    String rest = part.length() <= 1 ? "" : part.substring(1);
                    builder.append(start.toUpperCase(locale)).append(rest.toLowerCase(locale));
                }
                return builder.toString();
            }
            case SNAKE_CASE -> {
                String[] parts = plainText.split(" ");
                StringBuilder builder = new StringBuilder();
                for (String part : parts) {
                    builder.append(builder.isEmpty() ? "" : "_").append(part.toLowerCase(locale));
                }
                return builder.toString().replaceAll("_{2,}", " ");
            }
            case KEBAB_CASE -> {
                String[] parts = plainText.split(" ");
                StringBuilder builder = new StringBuilder();
                for (String part : parts) {
                    builder.append(builder.isEmpty() ? "" : "-").append(part.toLowerCase(locale));
                }
                return builder.toString().replaceAll("-{2,}", " ");
            }
            case PLAIN_TEXT -> {
                return plainText;
            }
            case MIXED_CASE_TYPES -> throw new IllegalArgumentException(NamingConvention.MIXED_CASE_TYPES + " is allowed as input only");
            case null, default -> throw new IllegalArgumentException("Invalid naming convention: " + convention);
        }
    }

    /**
     * The different naming conventions supported by this class.
     */
    public enum NamingConvention {
        /**
         * <b>Classis plain text.</b>
         * <p>
         * It contains spaces and upper-/lowercases in every possible combination; no restrictions.
         */
        PLAIN_TEXT("Plain text"),
        /**
         * <b>Camel cased text.</b>
         * <p>
         * It starts with a lowercase letter, words are separated by their uppercased first letter; no whitespaces, hyphens or underscores
         */
        CAMEL_CASE("camelCase"),
        /**
         * <b>Pascal cased text.</b>
         * <p>
         * It starts with an uppercase letter, words are separated by their uppercased first letter; no whitespaces, hyphens or underscores
         */
        PASCAL_CASE("PascalCase"),
        /**
         * <b>Snake cased text.</b>
         * <p>
         * All letters are lowercased, words are separated by underscores; no uppercase letters, whitespaces or hyphens
         */
        SNAKE_CASE("snake_case"),
        /**
         * <b>Kebab cased text.</b>
         * <p>
         * All letters are lowercased, words are separated by hyphens; no uppercase letters, whitespaces or underscores
         */
        KEBAB_CASE("kebab-case"),
        /**
         * <b>Text with mixed case types</b>
         * <p>
         * Words can be separated by uppercased letters, hyphens or underscores; no whitespaces
         */
        MIXED_CASE_TYPES("mixed-caseTypes");

        /**
         * The naming convention label. It represents the convention's name written in that very convention.
         */
        private final String label;

        /**
         * Constructs a new Naming Conventions
         * @param label The convention's name written in the convention.
         */
        NamingConvention(String label) {
            this.label = label;
        }

        /**
         * Returns the convention's {@link #label label}.
         */
        @Override
        public String toString() {
            return label;
        }
    }
}
