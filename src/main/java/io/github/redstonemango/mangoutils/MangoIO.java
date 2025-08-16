/*
 * Copyright (c) 2025 RedStoneMango
 *
 * This file is licensed under the MIT License.
 * You may use, copy, modify, and distribute this file under the terms of the MIT License.
 * See the LICENSE file or https://opensource.org/licenses/MIT for full text.
 */

package io.github.redstonemango.mangoutils;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
     * @see #deleteDirectoryRecursively(Path)
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
     * @see #deleteDirectoryRecursively(File)
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

    /**
     * Recursively copies a directory and all its contents from a {@link File} source to a {@link File} target.
     * <p>
     * This method walks the file tree rooted at {@code source}, creating directories and copying files
     * into the {@code target} path. Existing files are overwritten.
     * </p>
     *
     * @param source the source directory to copy
     * @param target the destination directory
     * @throws IOException if an I/O error occurs during copying
     * @see #copyDirectoryRecursively(Path, Path)
     */
    public static void copyDirectoryRecursively(File source, File target) throws IOException {
        copyDirectoryRecursively(source.toPath(), target.toPath());
    }
    /**
     * Recursively copies a directory and all its contents from a {@link Path} source to a {@link Path} target.
     * <p>
     * This method walks the file tree rooted at {@code source}, creating directories and copying files
     * into the {@code target} path. Existing files are overwritten.
     * </p>
     *
     * @param source the source directory path to copy
     * @param target the destination directory path
     * @throws IOException if an I/O error occurs during copying
     * @throws IllegalArgumentException if the source is not a directory
     * @see #copyDirectoryRecursively(File, File)
     */
    public static void copyDirectoryRecursively(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("Source must exist and be a directory: " + source);
        }
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Recursively moves a directory and all its contents from a {@link File} source to a {@link File} target.
     * <p>
     * This method first copies the source directory to the target location and then deletes the source.
     * </p>
     *
     * @param source the source directory file to move
     * @param target the target directory file
     * @throws IOException if an I/O error occurs during copying or deletion
     * @see #moveDirectoryRecursively(Path, Path)
     */
    public static void moveDirectoryRecursively(File source, File target) throws IOException {
        moveDirectoryRecursively(source.toPath(), target.toPath());
    }
    /**
     * Recursively moves a directory and all its contents from a {@link Path} source to a {@link Path} target.
     * <p>
     * This method first copies the source directory to the target location and then deletes the source.
     * </p>
     *
     * @param source the source directory path to move
     * @param target the target directory path
     * @throws IOException if an I/O error occurs during copying or deletion
     * @see #moveDirectoryRecursively(File, File)
     */
    public static void moveDirectoryRecursively(Path source, Path target) throws IOException {
        copyDirectoryRecursively(source, target);
        deleteDirectoryRecursively(source);
    }

    /**
     * Returns the next available {@link File} by appending a numeric suffix if the base file already exists.
     * <p>
     * For example, if "file.txt" exists, it will return "file (1).txt", "file (2).txt", etc.
     * </p>
     *
     * @param baseFile the base file to check
     * @return a new {@link File} object with a non-existing file name
     * @see #getNextAvailablePath(Path)
     */
    public static File getNexAvailableFile(File baseFile) {
        return getNextAvailablePath(baseFile.toPath()).toFile();
    }
    /**
     * Returns the next available {@link Path} by appending a numeric suffix if the base path already exists.
     * <p>
     * For example, if "file.txt" exists, it will return "file (1).txt", "file (2).txt", etc.
     * </p>
     *
     * @param basePath the base path to check
     * @return a new {@link Path} object with a non-existing file name
     */
    public static Path getNextAvailablePath(Path basePath) {
        Path path = basePath;
        String name = basePath.getFileName().toString();
        Path parent = basePath.getParent();
        String baseName;
        String extension = "";

        int dotIndex = name.lastIndexOf('.');
        if (dotIndex != -1) {
            baseName = name.substring(0, dotIndex);
            extension = name.substring(dotIndex);
        } else {
            baseName = name;
        }

        int count = 1;
        while (Files.exists(path)) {
            String newName = String.format(Locale.ROOT, "%s (%d)%s", baseName, count, extension);
            if (parent != null) {
                path = parent.resolve(newName);
            } else {
                path = Paths.get(newName);
            }
            count++;
        }

        return path;
    }

    /**
     * Compresses the specified {@link File} into a ZIP archive.
     * <p>
     * The source file or directory is compressed into the target ZIP file. The root folder is excluded.
     * </p>
     *
     * @param sourceFile the source file or directory to compress
     * @param zipFile the ZIP file to create
     * @throws IOException if an I/O error occurs during compression
     * @see #compressFile(File, File, boolean)
     * @see #compressPath(Path, Path)
     */
    public static void compressFile(File sourceFile, File zipFile) throws IOException {
        compressPath(sourceFile.toPath(), zipFile.toPath());
    }
    /**
     * Compresses the specified {@link File} into a ZIP archive with an option to include the root folder.
     * <p>
     * The source file or directory is compressed into the target ZIP file.
     * </p>
     *
     * @param sourceFile the source file or directory to compress
     * @param zipFile the ZIP file to create
     * @param includeRootFolder whether to include the root folder in the ZIP archive
     * @throws IOException if an I/O error occurs during compression
     * @see #compressPath(Path, Path, boolean)
     */
    public static void compressFile(File sourceFile, File zipFile, boolean includeRootFolder) throws IOException {
        compressPath(sourceFile.toPath(), zipFile.toPath(), includeRootFolder);
    }
    /**
     * Compresses the specified {@link Path} into a ZIP archive.
     * <p>
     * The source path is compressed into the target ZIP file. The root folder is excluded.
     * </p>
     *
     * @param sourcePath the source path to compress
     * @param zipPath the ZIP file to create
     * @throws IOException if an I/O error occurs during compression
     * @see #compressPath(Path, Path, boolean)
     * @see #compressFile(File, File)
     */
    public static void compressPath(Path sourcePath, Path zipPath) throws IOException {
        compressPath(sourcePath, zipPath, false);
    }
    /**
     * Compresses the specified {@link Path} into a ZIP archive with an option to include the root folder.
     * <p>
     * If the source is a directory, its contents are recursively added to the ZIP. The root folder can optionally be included.
     * </p>
     *
     * @param sourcePath the source path to compress
     * @param zipPath the ZIP file to create
     * @param includeRootFolder whether to include the root folder in the ZIP archive
     * @throws IOException if an I/O error occurs during compression
     * @see #compressFile(File, File, boolean)
     */
    public static void compressPath(Path sourcePath, Path zipPath, boolean includeRootFolder) throws IOException {
        if (!Files.exists(sourcePath)) {
            throw new FileNotFoundException("Path does not exist: " + sourcePath);
        }

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            if (Files.isDirectory(sourcePath)) {
                try (Stream<Path> stream = Files.walk(sourcePath)) {
                    stream.forEach(p -> {
                        try {
                            if (Files.isSameFile(p, sourcePath)) {
                                if (includeRootFolder) {
                                    String dirName = sourcePath.getFileName().toString();
                                    zos.putNextEntry(new ZipEntry(dirName + "/"));
                                    zos.closeEntry();
                                }
                                return;
                            }

                            Path relativePath = sourcePath.relativize(p);
                            String zipEntryName = includeRootFolder
                                    ? sourcePath.getFileName().resolve(relativePath).toString().replace("\\", "/")
                                    : relativePath.toString().replace("\\", "/");

                            if (Files.isDirectory(p)) {
                                zos.putNextEntry(new ZipEntry(zipEntryName + "/"));
                                zos.closeEntry();
                            } else {
                                zos.putNextEntry(new ZipEntry(zipEntryName));
                                Files.copy(p, zos);
                                zos.closeEntry();
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }
            } else {
                String zipEntryName = includeRootFolder
                        ? sourcePath.getParent().relativize(sourcePath).toString().replace("\\", "/")
                        : sourcePath.getFileName().toString();

                zos.putNextEntry(new ZipEntry(zipEntryName));
                Files.copy(sourcePath, zos);
                zos.closeEntry();
            }
        }
    }

    /**
     * Extracts the contents of a ZIP {@link File} to the specified target directory.
     * <p>
     * The ZIP is extracted into the target directory without stripping a possible root folder.
     * </p>
     *
     * @param zipFile the ZIP file to extract
     * @param targetFile the target directory to extract the contents into
     * @throws IOException if an I/O error occurs during extraction
     * @see #extractZipFile(File, File, boolean)
     * @see #extractZipPath(Path, Path)
     */
    public static void extractZipFile(File zipFile, File targetFile) throws IOException {
        extractZipPath(zipFile.toPath(), targetFile.toPath());
    }
    /**
     * Extracts the contents of a ZIP {@link File} to the specified target directory with an option to strip the root folder.
     * <p>
     * If {@code stripRootFolder} is true, the top-level folder inside the ZIP is removed from the extracted path if such a folder exists.
     * </p>
     *
     * @param zipFile the ZIP file to extract
     * @param targetFile the target directory to extract the contents into
     * @param stripRootFolder whether to remove the root folder from the extracted paths
     * @throws IOException if an I/O error occurs during extraction
     * @see #extractZipPath(Path, Path, boolean)
     */
    public static void extractZipFile(File zipFile, File targetFile, boolean stripRootFolder) throws IOException {
        extractZipPath(zipFile.toPath(), targetFile.toPath(), stripRootFolder);
    }
    /**
     * Extracts the contents of a ZIP {@link Path} to the specified target directory.
     * <p>
     * The ZIP is extracted into the target directory without stripping a possible root folder.
     * </p>
     *
     * @param zipPath the ZIP file to extract
     * @param targetDir the target directory to extract the contents into
     * @throws IOException if an I/O error occurs during extraction
     * @see #extractZipPath(Path, Path, boolean)
     * @see #extractZipFile(File, File)
     */
    public static void extractZipPath(Path zipPath, Path targetDir) throws IOException {
        extractZipPath(zipPath, targetDir, false);
    }
    /**
     * Extracts the contents of a ZIP {@link Path} to the specified target directory with an option to strip the root folder.
     * <p>
     * If {@code stripRootFolder} is true, the top-level folder inside the ZIP is removed from the extracted path if such a folder exists.
     * </p>
     *
     * @param zipPath the ZIP file to extract
     * @param targetDir the target directory to extract the contents into
     * @param stripRootFolder whether to remove the root folder from the extracted paths
     * @throws IOException if an I/O error occurs during extraction or the ZIP file is malformed
     * @see #extractZipFile(File, File, boolean)
     */
    public static void extractZipPath(Path zipPath, Path targetDir, boolean stripRootFolder) throws IOException {
        if (!Files.exists(zipPath)) {
            throw new FileNotFoundException("ZIP file does not exist: " + zipPath);
        }

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            String rootPrefix = null;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                entryName = entryName.replace("\\", "/");

                if (stripRootFolder) {
                    if (rootPrefix == null) {
                        int slashIndex = entryName.indexOf('/');
                        if (slashIndex > 0) {
                            rootPrefix = entryName.substring(0, slashIndex + 1);
                        } else {
                            rootPrefix = "";
                        }
                    }

                    if (entryName.startsWith(rootPrefix)) {
                        entryName = entryName.substring(rootPrefix.length());
                    }
                }

                if (entryName.isEmpty()) {
                    continue;
                }

                Path outputPath = targetDir.resolve(entryName).normalize();

                if (!outputPath.startsWith(targetDir)) {
                    throw new IOException("Bad ZIP entry: " + entryName);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    try (OutputStream os = Files.newOutputStream(outputPath)) {
                        zis.transferTo(os);
                    }
                }
                zis.closeEntry();
            }
        }
    }
}