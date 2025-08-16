/*
 * Copyright (c) 2025 RedStoneMango
 *
 * This file is licensed under the MIT License.
 * You may use, copy, modify, and distribute this file under the terms of the MIT License.
 * See the LICENSE file or https://opensource.org/licenses/MIT for full text.
 */

package io.github.redstonemango.mangoutils;

import org.jetbrains.annotations.Nullable;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Provides utility methods for encrypting and decrypting strings using AES-GCM with
 * password-based key derivation (PBKDF2 with HMAC SHA-256).
 * <p>
 * The encrypted data format combines the salt, IV (initialization vector), and ciphertext.
 * This class supports encryption and decryption using either a password or a pre-derived {@link SecretKey}.
 * </p>
 *
 * <p><strong>Note:</strong> GCM mode provides confidentiality and integrity but not authentication of the origin.</p>
 *
 * @author RedStoneMango
 */
public class CypherEncryption {

    /**
     * Size of the encryption key in bits.
     * This is AES-128.
     */
    private static final int KEY_SIZE = 128;
    /**
     * Size of the Initialization Vector (IV) in bytes.
     * 12 bytes (96 bits) is standard for AES-GCM mode.
     */
    private static final int IV_SIZE = 12;
    /**
     * Length of the authentication tag in bits.
     * Used for data integrity in AES-GCM.
     */
    private static final int TAG_LENGTH = 128;
    /**
     * Number of iterations for the key derivation function (PBKDF2).
     * Higher values increase computational cost, improving security.
     */
    private static final int ITERATIONS = 65536;
    /**
     * Length of the cryptographic salt in bytes.
     * Used to randomize key derivation and prevent precomputed attacks.
     */
    private static final int SALT_LENGTH = 16;
    
    /**
     * Encrypts a byte array using a password and returns the raw encrypted bytes.
     *
     * @param inputBytes    The byte array to encrypt.
     * @param password The password to derive the encryption key.
     * @return A byte array containing salt + IV + ciphertext.
     * @throws Exception If encryption fails.
     */
    public static byte[] encrypt(byte[] inputBytes, char[] password) throws Exception {
        byte[] salt = generateRandomBytes(SALT_LENGTH);
        byte[] iv = generateRandomBytes(IV_SIZE);

        SecretKey key = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] encryptedBytes = cipher.doFinal(inputBytes);

        // Combine salt + IV + ciphertext
        byte[] combined = new byte[salt.length + iv.length + encryptedBytes.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, salt.length + iv.length, encryptedBytes.length);

        return combined;
    }
    /**
     * Encrypts a byte array using a given {@link SecretKey} and salt.
     *
     * @param inputBytes The byte array to encrypt.
     * @param key   The encryption key.
     * @param salt  The salt to prepend to the result.
     * @return The encrypted result as a byte array: salt + IV + ciphertext.
     * @throws Exception If encryption fails.
     */
    public static byte[] encrypt(byte[] inputBytes, SecretKey key, byte[] salt) throws Exception {
        byte[] iv = generateRandomBytes(IV_SIZE);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] encryptedBytes = cipher.doFinal(inputBytes);

        // Combine salt + IV + ciphertext
        byte[] combined = new byte[salt.length + iv.length + encryptedBytes.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, salt.length + iv.length, encryptedBytes.length);

        return combined;
    }
    
    /**
     * Decrypts a byte array that includes salt, IV, and ciphertext using a password.
     * <p>
     * Returns null if decryption fails due to an invalid authentication tag.
     * </p>
     *
     * @param cypherBytes The encrypted byte array (salt + IV + ciphertext).
     * @param password    The password to derive the decryption key.
     * @return The decrypted byte array or null if decryption fails due to an invalid authentication tag.
     * @throws Exception If decryption fails due to an internal error.
     */
    public static byte@Nullable[] decrypt(byte[] cypherBytes, char[] password) throws Exception {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_SIZE];
            byte[] ciphertext = new byte[cypherBytes.length - SALT_LENGTH - IV_SIZE];

            System.arraycopy(cypherBytes, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(cypherBytes, SALT_LENGTH, iv, 0, IV_SIZE);
            System.arraycopy(cypherBytes, SALT_LENGTH + IV_SIZE, ciphertext, 0, ciphertext.length);

            SecretKey key = deriveKey(password, salt);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException e) {
            return null;
        }
    }
    /**
     * Decrypts a byte array that includes IV + ciphertext using a pre-derived key.
     * <p>
     * Returns null if decryption fails due to an invalid authentication tag.
     * </p>
     *
     * @param cypherBytes The encrypted byte array (salt + IV + ciphertext).
     * @param key         The AES key used for decryption.
     * @return The decrypted byte array, or if decryption fails due to an invalid authentication tag.
     * @throws Exception If decryption fails due to an internal error.
     */
    public static byte@Nullable[] decrypt(byte[] cypherBytes, SecretKey key) throws Exception {
        try {
            int offset = SALT_LENGTH;

            byte[] iv = new byte[IV_SIZE];
            byte[] ciphertext = new byte[cypherBytes.length - offset - IV_SIZE];

            System.arraycopy(cypherBytes, offset, iv, 0, IV_SIZE);
            System.arraycopy(cypherBytes, offset + IV_SIZE, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException e) {
            return null;
        }
    }

    /**
     * Generates a random cryptographic salt.
     *
     * @return A new random salt byte array.
     */
    public static byte[] generateRandomSalt() {
        return generateRandomBytes(SALT_LENGTH);
    }

    /**
     * Extracts the salt from a cypher byte array and derives a key using the given password.
     *
     * @param cypherBytes The encrypted byte array (starting with the salt).
     * @param password    The password used for key derivation.
     * @return The derived AES key.
     * @throws Exception If key derivation fails.
     */
    public static SecretKey extractAndDeriveKey(byte[] cypherBytes, char[] password) throws Exception {
        byte[] salt = Arrays.copyOfRange(cypherBytes, 0, SALT_LENGTH);
        return deriveKey(password, salt);
    }

    /**
     * Extracts the salt from the given cipher byte array by copying the salt prefix into a separate array.
     * <p>
     * <strong>Note:</strong> This method does not edit the original {@code cypherBytes}. The salt is still included in the original array after this method is finished.
     * </p>
     *
     * @param cypherBytes The full encrypted byte array containing salt + IV + ciphertext.
     * @return A byte array containing only the IV + ciphertext portion (payload).
     */
    public static byte[] extractSalt(byte[] cypherBytes) {
        return Arrays.copyOfRange(cypherBytes, 0, SALT_LENGTH);
    }
    /**
     * Derives an AES secret key from the given password and salt using PBKDF2 with HMAC SHA-256.
     *
     * @param password The password to derive the key from.
     * @param salt     The salt to use for key derivation.
     * @return A {@link SecretKey} suitable for AES encryption.
     * @throws Exception If the key derivation algorithm is not available or fails.
     */
    public static SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
    /**
     * Generates a random byte array of the specified length using a cryptographically secure random number generator.
     *
     * @param length The length of the byte array to generate.
     * @return A byte array filled with securely generated random bytes.
     */
    private static byte[] generateRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }
}
