/*
 * Copyright (c) 2025 RedStoneMango
 *
 * This file is licensed under the MIT License.
 * You may use, copy, modify, and distribute this file under the terms of the MIT License.
 * See the LICENSE file or https://opensource.org/licenses/MIT for full text.
 */

package io.github.redstonemango.mangoutils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Utility class for file and directory operations.
 *
 * @author RedStoneMango
 */
public class MangoIO {

    /**
     * Recursively deletes a directory and all its contents given a {@link File} object.
     * <p>
     * This method converts the {@code File} to a {@link Path} and delegates
     * the deletion process to {@link #deleteDirectoryRecursively(Path)}.
     * </p>
     *
     * @param directory the directory to delete recursively
     * @throws IOException if an I/O error occurs during deletion
     */
    public static void deleteDirectoryRecursively(File directory) throws IOException {
        deleteDirectoryRecursively(directory.toPath());
    }

    /**
     * Recursively deletes a directory and all its contents given a {@link Path} object.
     * <p>
     * The method first checks if the directory exists. If it does, it walks
     * the file tree starting at the directory and deletes all files and subdirectories,
     * processing children before parents (depth-first).
     * </p>
     *
     * @param directory the path to the directory to delete recursively
     * @throws IOException if an I/O error occurs during deletion
     *                     or if the directory cannot be accessed
     * @throws RuntimeException if a file or directory cannot be deleted
     */
    public static void deleteDirectoryRecursively(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> fileStream = Files.walk(directory)) {
            fileStream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete " + path, e);
                        }
                    });
        }
    }
}