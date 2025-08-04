/*
 * Copyright (c) 2025 RedStoneMango
 *
 * This file is licensed under the MIT License.
 * You may use, copy, modify, and distribute this file under the terms of the MIT License.
 * See the LICENSE file or https://opensource.org/licenses/MIT for full text.
 */

package io.github.redstonemango.mangoutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Powerful command line argument parser that supports flags, key-value pairs, aliases,
 * and literal arguments while providing flexible parsing and error handling.
 * <p>
 * <strong>Term Definitions:</strong>
 *
 * <table border="1" cellpadding="4" cellspacing="0">
 *     <thead>
 *         <tr>
 *             <th>Term</th>
 *             <th>Definition</th>
 *             <th>Example</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td><strong>Key</strong></td>
 *             <td>A keyword (prefixed with <code>--</code>) that expects an associated value.</td>
 *             <td><code>--input file.txt</code></td>
 *         </tr>
 *         <tr>
 *             <td><strong>Flag</strong></td>
 *             <td>A keyword (prefixed with <code>--</code>) that has no value and functions as a boolean switch.</td>
 *             <td><code>--verbose</code></td>
 *         </tr>
 *         <tr>
 *             <td><strong>Alias</strong></td>
 *             <td>A single-character shorthand (prefixed with <code>-</code> or part of an alias chain) representing a key or flag.</td>
 *             <td><code>-v</code>, <code>-i file.txt</code></td>
 *         </tr>
 *         <tr>
 *             <td><strong>Alias Chain</strong></td>
 *             <td>A sequence of aliases (prefixed with <code>-</code>) that may consist of multiple flags or end with a key followed by its value.</td>
 *             <td><code>-vhz</code>, <code>-vhi file.txt</code></td>
 *         </tr>
 *         <tr>
 *             <td><strong>Literal Argument</strong></td>
 *             <td>An argument that does not begin with a hyphen. Literal arguments are positional and order-sensitive.</td>
 *             <td><code>build</code></td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * @author RedStoneMango
 */
public class CliArg {

    /**
     * Error code indicating a key was provided without an associated value.
     */
    public static final int ERROR_NO_KEY_VALUE = 0;
    /**
     * Error code indicating an unknown flag or key element was encountered.
     */
    public static final int ERROR_UNKNOWN_ELEMENT = 1;
    /**
     * Error code indicating an invalid alias (short form) was provided.
     */
    public static final int ERROR_INVALID_ALIAS = 2;

    /**
     * Set of flags parsed from the arguments.
     */
    private final Set<String> flags = new HashSet<>();
    /**
     * Map of keys to their associated values parsed from the arguments.
     */
    private final Map<String, String> values = new HashMap<>();
    /**
     * List of literal arguments.
     */
    private final List<String> literalArgs = new ArrayList<>();

    /**
     * Private constructor to prevent direct instantiation.
     * Instances should be created via the {@link Compiler}.
     */
    private CliArg() {}

    /**
     * Creates a new Compiler instance to configure and parse arguments.
     *
     * @return a new Compiler instance
     */
    public static Compiler compiler() {
        return new Compiler();
    }

    /**
     * Parses a raw command line argument string into individual argument tokens.
     * Supports quoted strings and escaped characters.
     *
     * @param argString the raw argument string to parse
     * @return an array of parsed argument tokens
     */
    public static String[] parseArgString(String argString) {
        List<String> args = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder builder = new StringBuilder();

        for (char c : argString.toCharArray()) {
            if (c == '"') {
                if (!builder.isEmpty() && builder.charAt(builder.length() - 1) == '\\') {
                    builder.deleteCharAt(builder.length() - 1);
                }
                else {
                    inQuote = !inQuote;
                    continue;
                }
            }
            else {
                if (c == ' ') {
                    if (!inQuote) {
                        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) == '\\') {
                            builder.deleteCharAt(builder.length() - 1);
                        }
                        else {
                            args.add(builder.toString());
                            builder.setLength(0);
                            continue;
                        }
                    }
                }
                if (c != '\\') {
                    if (!builder.isEmpty() && builder.charAt(builder.length() - 1) == '\\') {
                        builder.deleteCharAt(builder.length() - 1);
                    }
                }
            }
            builder.append(c);
        }
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) == '\\') {
            builder.deleteCharAt(builder.length() - 1);
        }
        args.add(builder.toString());
        return args.toArray(String[]::new);
    }

    /**
     * Constructs a formatted error message based on the error type and cause.
     *
     * @param cause the key, alias, or element that caused the error
     * @param type one of the error code constants (ERROR_NO_KEY_VALUE, ERROR_UNKNOWN_ELEMENT, etc.)
     * @return a formatted error message string describing the error
     * @throws IllegalArgumentException if the error type is invalid or unknown
     */
    public static String constructErrorMessage(String cause, int type) throws IllegalArgumentException {
        return switch (type) {
            case ERROR_NO_KEY_VALUE -> "Key '" + cause + "' does not have a value assigned to it";
            case ERROR_UNKNOWN_ELEMENT -> "Unknown flag / key: '" + cause + "'";
            case ERROR_INVALID_ALIAS -> "Unknown alias: '" + cause + "'";
            default -> throw new IllegalArgumentException("Error type '" + type + "' does not exist");
        };
    }

    /**
     * Checks if a specific flag was set in the parsed arguments.
     *
     * @param flag the flag to check for
     * @return true if the flag was present, false otherwise
     */
    public boolean isFlag(String flag) {
        return flags.contains(flag);
    }
    /**
     * Checks if a key with an associated value was parsed.
     *
     * @param key the key to check for
     * @return true if the key exists with a value, false otherwise
     */
    public boolean hasValueKey(String key) {
        return values.containsKey(key);
    }
    /**
     * Checks if a specific value was parsed for any key.
     *
     * @param value the value to check for
     * @return true if any key is assigned this value, false otherwise
     */
    public boolean hasValue(String value) {
        return values.containsValue(value);
    }
    /**
     * Retrieves the value associated with a specific key.
     *
     * @param key the key whose value is requested
     * @return the value assigned to the key, or null if none exists
     */
    public String getValue(String key) {
        return values.get(key);
    }
    /**
     * Checks if an literal argument exists at the specified index.
     *
     * @param index the index to check
     * @return true if a literal argument exists at that index, false otherwise
     */
    public boolean hasLiteralArg(int index) {
        return literalArgs.size() > index;
    }
    /**
     * Checks if a literal argument with the specified value exists.
     *
     * @param arg the argument to check for
     * @return true if the literal argument exists, false otherwise
     */
    public boolean hasLiteralArg(String arg) {
        return literalArgs.contains(arg);
    }
    /**
     * Retrieves the literal argument at the specified index.
     *
     * @param index the index of the literal argument
     * @return the literal argument string at the index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public String getLiteralArg(int index) {
        return literalArgs.get(index);
    }
    /**
     * Returns an unmodifiable view of the parsed flags.
     *
     * @return an unmodifiable Set containing all flags
     */
    public Set<String> getFlagsUnmodifiable() {
        return Collections.unmodifiableSet(flags);
    }
    /**
     * Returns an unmodifiable view of the parsed key-value pairs.
     *
     * @return an unmodifiable Map of keys and their associated values
     */
    public Map<String, String> getKeyValuePairsUnmodifiable() {
        return Collections.unmodifiableMap(values);
    }
    /**
     * Returns an unmodifiable view of the literal arguments.
     *
     * @return an unmodifiable List of literal arguments
     */
    public List<String> getLiteralArgsUnmodifiable() {
        return Collections.unmodifiableList(literalArgs);
    }

    /**
     * Returns a string representation of the parsed arguments including flags,
     * key-value pairs, and literal arguments.
     *
     * @return a formatted string describing the parsed arguments
     */
    @Override
    public String toString() {
        return String.format("CliArgs{Flags%s || Values%s || Literal%s}", flags, values, literalArgs);
    }

    /**
     * A builder-style compiler class used to configure the argument parser
     * and perform the actual parsing/compilation of argument arrays.
     */
    public static class Compiler {
        /**
         * Set of keys that expect an associated value.
         */
        private final Set<String> keys = new HashSet<>();
        /**
         * Set of recognized flags that do not take values.
         */
        private final Set<String> flags = new HashSet<>();
        /**
         * Map of short-form aliases (characters) to long-form keys or flags.
         */
        private final Map<Character, String> aliases = new HashMap<>();
        /**
         * Flag indicating whether unknown flags/keys should be stored as flags
         * instead of causing errors during compilation.
         */
        private boolean storeUnresolvedAsFlag = false;

        /**
         * Compiles the given argument array into a CliArg instance.
         * Throws an exception if an error occurs.
         *
         * @param args the argument array to compile
         * @return a CliArg containing the parsed results
         * @throws CompilationException if invalid arguments are detected
         */
        public CliArg compile(String[] args) throws CompilationException {
            return compileInternally(args, (s, i) -> {}, false);
        }

        /**
         * Compiles the argument array into a CliArgParser instance, swallowing errors.
         * Any errors encountered will be handled silently, preventing exceptions and allowing the caller to continue uninterrupted.
         *
         * @param args the argument array to compile
         * @return a CliArgParser instance, possibly empty if errors occurred
         */
        public CliArg compileSafely(String[] args) {
            try {
                return compileInternally(args, (s, i) -> {}, true);
            }
            catch (CompilationException e) {
                return new CliArg(); // Exception will never be thrown, for we called the method with an active failsafe flag. This line just exists to make the compiler happy
            }
        }

        /**
         * Compiles the argument array, providing error callback handling, swallowing errors.
         * Any errors encountered will be delegated to the error callback, preventing exceptions and allowing the caller to continue uninterrupted.
         *
         * @param args the argument array to compile
         * @param onError a callback invoked with error cause and type for each error
         * @return a CliArgParser instance, possibly empty if errors occurred
         */
        public CliArg compileSafely(String[] args, BiConsumer<String, Integer> onError) {
            try {
                return compileInternally(args, onError, true);
            }
            catch (CompilationException e) {
                return new CliArg(); // Exception will never be thrown, for we called the method with an active failsafe flag. This line just exists to make the compiler happy (Déjà vu...)
            }
        }

        /**
         * Internal compilation method used by public compile methods.
         * Parses the argument array according to the configured keys, flags, and aliases.
         *
         * @param args the argument array to parse
         * @param onError callback for error reporting
         * @param failsafe if true, parsing continues despite errors; otherwise throws exceptions
         * @return a CliArgParser containing parsed arguments
         * @throws CompilationException if failsafe is false and a parsing error occurs
         */
        private CliArg compileInternally(String[] args, BiConsumer<String, Integer> onError, boolean failsafe) throws CompilationException {
            CliArg result = new CliArg();
            String currentKey = null;

            for (String arg : args) {
                if (arg.startsWith("-") && currentKey != null) {
                    if (!failsafe) throw new CompilationException(constructErrorMessage(currentKey, ERROR_NO_KEY_VALUE));
                    onError.accept(currentKey, ERROR_NO_KEY_VALUE);
                    currentKey = null;
                }

                if (arg.startsWith("--")) {
                    String literal = arg.substring("--".length());
                    if (flags.contains(literal)) {
                        result.flags.add(literal);
                    }
                    else if (keys.contains(literal)) {
                        currentKey = literal;
                    }
                    else {
                        if (storeUnresolvedAsFlag) {
                            result.flags.add(literal);
                        }
                        else {
                            if (!failsafe) throw new CompilationException(constructErrorMessage(arg, ERROR_UNKNOWN_ELEMENT));
                            onError.accept(arg, ERROR_UNKNOWN_ELEMENT);
                        }
                    }
                }
                else if (arg.startsWith("-")) {
                    for (char c : arg.substring("-".length()).toCharArray()) {
                        if (currentKey != null) {
                            if (!failsafe) throw new CompilationException(constructErrorMessage(currentKey, ERROR_NO_KEY_VALUE));
                            onError.accept(currentKey, ERROR_NO_KEY_VALUE);
                            currentKey = null;
                        }

                        String longForm = aliases.get(c);
                        if (longForm == null) {
                            if (!storeUnresolvedAsFlag) {
                                if (!failsafe) throw new CompilationException(constructErrorMessage("-" + c, ERROR_INVALID_ALIAS));
                                onError.accept("-" + c, ERROR_INVALID_ALIAS);
                            }
                            else {
                                result.flags.add(String.valueOf(c));
                            }
                            continue;
                        }
                        if (!(keys.contains(longForm) || flags.contains(longForm))) {
                            if (!storeUnresolvedAsFlag) {
                                if (!failsafe) throw new CompilationException(constructErrorMessage("--" + longForm, ERROR_UNKNOWN_ELEMENT));
                                onError.accept("--" + longForm, ERROR_UNKNOWN_ELEMENT);
                            }
                            else {
                                result.flags.add(String.valueOf(c));
                            }
                            continue;
                        }

                        if (flags.contains(longForm)) {
                            result.flags.add(longForm);
                        }
                        else {
                            currentKey = longForm;
                        }
                    }
                }
                else {
                    if (currentKey != null) {
                        result.values.put(currentKey, arg);
                        currentKey = null;
                    } else {
                        result.literalArgs.add(arg);
                    }
                }
            }

            if (currentKey != null) {
                if (!failsafe) throw new CompilationException(constructErrorMessage(currentKey, ERROR_NO_KEY_VALUE));
                onError.accept(currentKey, ERROR_NO_KEY_VALUE);
                currentKey = null;
            }

            return result;
        }

        /**
         * Returns the set of keys configured for this compiler.
         *
         * @return an unmodifiable set of keys expecting values
         */
        public Set<String> getKeys() {
            return keys;
        }
        /**
         * Returns the set of flags configured for this compiler.
         *
         * @return an unmodifiable set of recognized flags
         */
        public Set<String> getFlags() {
            return flags;
        }
        /**
         * Returns the map of character aliases to long-form keys or flags.
         *
         * @return an unmodifiable map of aliases
         */
        public Map<Character, String> getAliases() {
            return aliases;
        }
        /**
         * Returns whether unresolved flags or keys are stored as flags.
         *
         * @return true if unresolved elements are stored as flags, false otherwise
         */
        public boolean storesUnresolvedAsFlag() {
            return storeUnresolvedAsFlag;
        }

        /**
         * Adds keys that expect associated values to the compiler configuration.
         *
         * @param keys one or more keys to register as expecting values
         * @return this Compiler instance for method chaining
         */
        public Compiler forKeys(String... keys) {
            this.keys.addAll(List.of(keys));
            return this;
        }
        /**
         * Adds flags (keys without values) to the compiler configuration.
         *
         * @param flags one or more flag names to register
         * @return this Compiler instance for method chaining
         */
        public Compiler forFlags(String... flags) {
            this.flags.addAll(List.of(flags));
            return this;
        }
        /**
         * Registers a short-form alias for a long-form key or flag.
         * Throws an exception if the short form is already registered.
         *
         * @param shortForm the single-character alias
         * @param longForm the full key or flag name
         * @return this Compiler instance for method chaining
         * @throws IllegalArgumentException if the short form is already registered.
         */
        public Compiler withAlias(char shortForm, String longForm) throws IllegalArgumentException {
            if (aliases.containsKey(shortForm)) throw new IllegalArgumentException("Short form '" + shortForm + "' is already registered");
            aliases.put(shortForm, longForm);
            return this;
        }
        /**
         * Configures the compiler to store unresolved flags or keys as flags
         * instead of throwing errors during compilation.
         *
         * @return this Compiler instance for method chaining
         */
        public Compiler storeUnresolvedAsFlag() {
            storeUnresolvedAsFlag = true;
            return this;
        }
    }

    /**
     * Exception thrown when a compilation error occurs during argument parsing.
     */
    public static class CompilationException extends Exception {
        /**
         * Creates a new CompilationException with no detail message.
         */
        public CompilationException() {
            super();
        }
        /**
         * Creates a new CompilationException with the specified detail message.
         *
         * @param message the detail message
         */
        public CompilationException(String message) {
            super(message);
        }
        /**
         * Creates a new CompilationException with the specified detail message and cause.
         *
         * @param message the detail message
         * @param cause the cause of this exception
         */
        public CompilationException(String message, Throwable cause) {
            super(message, cause);
        }
        /**
         * Creates a new CompilationException with the specified cause.
         *
         * @param cause the cause of this exception
         */
        public CompilationException(Throwable cause) {
            super(cause);
        }
    }
}