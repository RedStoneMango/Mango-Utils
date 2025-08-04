/*
 * Copyright (c) 2025 RedStoneMango
 *
 * This file is licensed under the MIT License.
 * You may use, copy, modify, and distribute this file under the terms of the MIT License.
 * See the LICENSE file or https://opensource.org/licenses/MIT for full text.
 */

package io.github.redstonemango.mangoutils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Utility class for securely hashing and verifying passwords using PBKDF2 with HMAC SHA-256.
 * <p>
 * This class generates a random salt and uses a high iteration count to slow down brute-force attacks.
 * The final format of the hashed password is: {@code base64(salt):iterations:base64(hash)}.
 *
 * @author RedStoneMango
 */
public class Hasher {

    /** Secure random number generator for creating cryptographic salts. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Length of the salt in bytes (128 bits). */
    private static final int SALT_LENGTH = 16;
    /** Length of the derived key in bits. */
    private static final int KEY_LENGTH = 256;
    /** Number of iterations for PBKDF2. */
    private static final int ITERATIONS = 60_000;

    /**
     * Hashes a password using PBKDF2 with HMAC SHA-256.
     *
     * @param password The password to hash.
     * @return A string containing the Base64-encoded salt, iteration count, and Base64-encoded hash,
     *         separated by colons.
     * @throws Exception If an error occurs during hashing.
     */
    public static String hash(char[] password) throws Exception {
        byte[] salt = generateSalt();
        byte[] hash = pbkdf2Hash(password, salt, ITERATIONS);

        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        String encodedHash = Base64.getEncoder().encodeToString(hash);

        return encodedSalt + ":" + ITERATIONS + ":" + encodedHash;
    }

    /**
     * Derives a cryptographic hash using PBKDF2 with the specified parameters.
     *
     * @param input      The input character array (e.g., password).
     * @param salt       The salt to use for hashing.
     * @param iterations The number of iterations for the PBKDF2 function.
     * @return A byte array containing the derived key.
     * @throws Exception If the PBKDF2 algorithm is not available.
     */
    private static byte[] pbkdf2Hash(char[] input, byte[] salt, int iterations) throws Exception {
        KeySpec spec = new PBEKeySpec(input, salt, iterations, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    /**
     * Generates a new random salt.
     *
     * @return A byte array containing a new salt.
     */
    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Verifies an input password against a previously stored hashed value.
     *
     * @param input      The input password to verify.
     * @param storedHash The stored hash string in the format {@code base64(salt):iterations:base64(hash)}.
     * @return {@code true} if the input password matches the stored hash, {@code false} otherwise.
     * @throws Exception If verification fails due to a parsing or cryptographic error.
     */
    public static boolean verifyHash(char[] input, String storedHash) throws Exception {
        String[] parts = storedHash.split(":");
        if (parts.length != 3) return false;

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        int iterations = Integer.parseInt(parts[1]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

        byte[] actualHash = pbkdf2Hash(input, salt, iterations);

        return constantTimeEquals(expectedHash, actualHash);
    }

    /**
     * Compares two byte arrays in constant time to prevent timing attacks.
     *
     * @param a First byte array.
     * @param b Second byte array.
     * @return {@code true} if both arrays are equal in content and length, {@code false} otherwise.
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
