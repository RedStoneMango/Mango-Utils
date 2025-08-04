/*
 * Copyright (c) 2025 RedStoneMango
 *
 * This file is licensed under the MIT License.
 * You may use, copy, modify, and distribute this file under the terms of the MIT License.
 * See the LICENSE file or https://opensource.org/licenses/MIT for full text.
 */

package io.github.redstonemango.mangoutils;

import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents the current operating system and provides utility methods for OS-specific operations
 * such as opening files, browsing folders, or normalizing input events across platforms.
 * <p>
 * This enum acts as a safer and more predictable alternative to {@link java.awt.Desktop},
 * especially for Linux and Mac systems where {@code Desktop} is known to be unreliable.
 * </p>
 *
 * <p>Supported platforms:</p>
 * <ul>
 *     <li>{@link #WINDOWS}</li>
 *     <li>{@link #MAC}</li>
 *     <li>{@link #LINUX}</li>
 *     <li>{@link #UNKNOWN} (fallback, assumes a UNIX-Like system)</li>
 * </ul>
 *
 * @author RedStoneMango
 */

public enum OperatingSystem {

    /**
     * A Linux-based operating system (includes most Unix-like systems).
     */
    LINUX {
        @Override
        String[] createUriOpenCommand(URI uri) {
            String string = uri.toString();
            if ("file".equals(uri.getScheme())) {
                string = string.replace("file:", "file://");
            }

            return new String[]{"xdg-open", string};
        }

        @Nullable AtomicReference<String> usedManagerCache = null;

        @Override
        public String[] createFileBrowseCommand(File file) {
            String[] fileManagers = {"nautilus", "thuxnar", "dolphin", "caja", "io.elementary.files"};
            if (usedManagerCache == null) {
                usedManagerCache = new AtomicReference<>();
                for (String manager : fileManagers) {
                    if (isProcessExisting(manager)) {
                        usedManagerCache.set(manager);
                    }
                }
            }

            if (usedManagerCache.get() == null) return createUriOpenCommand(file.toURI());
            return new String[]{usedManagerCache.get(), "--select", file.getAbsolutePath()};
        }

        @Override
        public KeyEvent unifyKeyEvent(KeyEvent keyEvent) {
            return keyEvent;
        }

        @Override
        public MouseEvent unifyMouseEvent(MouseEvent mouseEvent) {
            return mouseEvent;
        }

        @Override
        public File createAppConfigDir(String folderName) {
            String userHome = System.getProperty("user.home");
            String configHome = System.getenv("XDG_CONFIG_HOME");
            if (configHome == null || configHome.isEmpty()) {
                configHome = userHome + "/.config";
            }
            return new File(configHome, folderName);
        }

        boolean isProcessExisting(String processName) {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"which", processName});
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                return reader.readLine() != null;
            } catch (IOException ignore) {
                return false;
            }
        }
    },
    /**
     * A Microsoft Windows-based operating system.
     */
    WINDOWS {
        @Override
        String[] createUriOpenCommand(URI uri) {
            return new String[]{"start", uri.toString()};
        }

        @Override
        String[] createFileBrowseCommand(File file) {
            return new String[]{"explorer.exe", "/select," + file.getAbsolutePath()};
        }

        @Override
        public KeyEvent unifyKeyEvent(KeyEvent keyEvent) {
            return keyEvent;
        }

        @Override
        public MouseEvent unifyMouseEvent(MouseEvent mouseEvent) {
            return mouseEvent;
        }

        @Override
        public File createAppConfigDir(String folderName) {
            String userHome = System.getProperty("user.home");
            return new File(new File(userHome, "AppData/Local"), folderName);
        }


    },
    /**
     * A macOS-based operating system developed by Apple Inc.
     */
    MAC {
        @Override
        String[] createUriOpenCommand(URI uri) {
            return new String[]{"open", uri.toString()};
        }

        @Override
        String[] createFileBrowseCommand(File file) {
            return new String[]{"open", "-R", file.getAbsolutePath()};
        }

        @Override
        public KeyEvent unifyKeyEvent(KeyEvent keyEvent) {
            int modifiers = 0;
            if (keyEvent.isMetaDown()) modifiers = modifiers | KeyEvent.CTRL_DOWN_MASK;
            if (keyEvent.isAltDown() || keyEvent.isAltGraphDown())  modifiers = modifiers | KeyEvent.ALT_DOWN_MASK | KeyEvent.ALT_GRAPH_DOWN_MASK;
            if (keyEvent.isControlDown()) modifiers = modifiers | KeyEvent.META_DOWN_MASK;
            if (keyEvent.isShiftDown()) modifiers = modifiers | KeyEvent.SHIFT_DOWN_MASK;

            return new KeyEvent(
                    keyEvent.getComponent(),
                    keyEvent.getID(),
                    keyEvent.getWhen(),
                    modifiers,
                    keyEvent.getKeyCode(),
                    keyEvent.getKeyChar(),
                    keyEvent.getKeyLocation()
            );
        }

        @Override
        public MouseEvent unifyMouseEvent(MouseEvent mouseEvent) {
            int modifiers = 0;
            if (mouseEvent.isMetaDown()) modifiers = modifiers | KeyEvent.CTRL_DOWN_MASK;
            if (mouseEvent.isAltDown() || mouseEvent.isAltGraphDown())  modifiers = modifiers | KeyEvent.ALT_DOWN_MASK | KeyEvent.ALT_GRAPH_DOWN_MASK;
            if (mouseEvent.isControlDown()) modifiers = modifiers | KeyEvent.META_DOWN_MASK;
            if (mouseEvent.isShiftDown()) modifiers = modifiers | KeyEvent.SHIFT_DOWN_MASK;

            return new MouseEvent(
                    mouseEvent.getComponent(),
                    mouseEvent.getID(),
                    mouseEvent.getWhen(),
                    modifiers,
                    mouseEvent.getX(),
                    mouseEvent.getY(),
                    mouseEvent.getClickCount(),
                    mouseEvent.isPopupTrigger()
            );
        }

        @Override
        public File createAppConfigDir(String folderName) {
            String userHome = System.getProperty("user.home");
            return new File(new File(userHome, "Library/Application Support"), folderName);
        }
    },
    /**
     * Represents an unknown or unsupported operating system.
     * <p>
     * Methods in this constant delegate to {@link #LINUX}, as Linux conventions are often
     * the most compatible fallback for open-source and Unix-like environments.
     * </p>
     */
    UNKNOWN {
        @Override
        String[] createUriOpenCommand(URI uri) {
            return LINUX.createUriOpenCommand(uri);
        }

        @Override
        String[] createFileBrowseCommand(File file) {
            return LINUX.createFileBrowseCommand(file);
        }

        @Override
        public KeyEvent unifyKeyEvent(KeyEvent keyEvent) {
            return keyEvent;
        }

        @Override
        public MouseEvent unifyMouseEvent(MouseEvent mouseEvent) {
            return mouseEvent;
        }

        @Override
        public File createAppConfigDir(String folderName) {
            return LINUX.createAppConfigDir(folderName);
        }
    };

    /**
     * Retrieves the value of the system property {@code os.name}.
     *
     * @return the name of the current operating system.
     */
    public static String readCurrentOSName() {
        return System.getProperty("os.name");
    }

    /**
     * Retrieves the value of the system property {@code os.version}.
     *
     * @return the version of the current operating system.
     */
    public static String readCurrentOSVersion() {
        return System.getProperty("os.version");
    }

    /**
     * Retrieves the value of the system property {@code os.arch}.
     *
     * @return the architecture of the current operating system.
     */
    public static String readCurrentOSArch() {
        return System.getProperty("os.arch");
    }

    /**
     * Attempts to detect and return the user's current operating system.
     * If the OS cannot be identified, {@link #UNKNOWN} is returned.
     *
     * @return the detected operating system.
     */
    public static OperatingSystem loadCurrentOS() {

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("nix") || os.contains("nux")) {
            return OperatingSystem.LINUX;
        }
        else if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        }
        else if (os.contains("mac")) {
            return OperatingSystem.MAC;
        }
        else {
            return OperatingSystem.UNKNOWN;
        }
    }

    /**
     * Checks whether the current OS is Windows.
     *
     * @return {@code true} if the OS is Windows; otherwise, {@code false}.
     * @see #loadCurrentOS()
     */
    public static boolean isLinux() {
        return loadCurrentOS() == LINUX;
    }

    /**
     * Checks whether the current OS is Windows.
     *
     * @return {@code true} if the OS is Windows; otherwise, {@code false}.
     * @see #loadCurrentOS()
     */
    public static boolean isWindows() {
        return loadCurrentOS() == WINDOWS;
    }

    /**
     * Checks whether the current OS is macOS.
     *
     * @return {@code true} if the OS is macOS; otherwise, {@code false}.
     * @see #loadCurrentOS()
     */
    public static boolean isMac() {
        return loadCurrentOS() == MAC;
    }

    /**
     * Checks whether the current OS is unknown.
     *
     * @return {@code true} if the OS is unrecognized; otherwise, {@code false}.
     * @see #loadCurrentOS()
     */
    public static boolean isUnknown() {
        return loadCurrentOS() == UNKNOWN;
    }

    /**
     * Opens the given file using the system's default application.
     * <p>This method opens the file directly (e.g., in a viewer or editor) but does not reveal it in a file explorer.</p>
     *
     * @param file the file to open.
     * @see #browse(File)
     */
    public void open(File file) {
        this.open(file.toURI());
    }

    /**
     * Opens the file at the specified path using the system's default application.
     *
     * @param path the file path to open.
     * @see #browse(File)
     */
    public void open(Path path) {
        this.open(path.toUri());
    }

    /**
     * Parses the given string as a {@link URI} and opens it using the system's default application.
     *
     * @param uri the string representation of the URI.
     * @throws IllegalArgumentException if the string cannot be parsed into a valid URI.
     * @see #browse(URI)
     */
    public void open(String uri) throws IllegalArgumentException {
        try {
            this.open(URI.create(uri));
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Unable to create URI from string '%s': %s", uri, e.getMessage()));
        }
    }

    /**
     * Opens the given URI using the system's default application.
     * <p>This can be used for both local file URIs and web URLs.</p>
     *
     * @param uri the URI to open.
     * @see #browse(URI)
     */
    public void open(URI uri) {
        try {
            Process process = Runtime.getRuntime().exec(this.createUriOpenCommand(uri));
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
        } catch (IOException ignore) {}
    }

    /**
     * Opens the file explorer and selects the file at the specified path.
     *
     * @param path the file path to browse.
     * @see #open(String)
     */
    public void browse(String path) {
        browse(new File(path));
    }

    /**
     * Opens the file explorer to show the file at the given URI.
     * <p>The URI must use the {@code file} scheme. Non-file URIs are not supported.</p>
     *
     * @param uri the URI to browse.
     * @throws IllegalArgumentException if the URI scheme is not {@code file}.
     * @see #open(URI)
     */
    public void browse(URI uri) {
        if ("file".equals(uri.getScheme())) {
            browse(new File(uri));
        }
        else {
            throw new IllegalArgumentException("Cannot browse a non-file URI");
        }
    }

    /**
     * Opens the file explorer to show the specified file.
     *
     * @param file the file to reveal.
     * @see #open(File)
     */
    public void browse(File file) {
        try {
            Process process = Runtime.getRuntime().exec(this.createFileBrowseCommand(file));
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructs a command line array used to open the given URI on the current operating system.
     *
     * @param uri the URI to open.
     * @return an array of command-line arguments suitable for {@link ProcessBuilder}.
     */
    abstract String[] createUriOpenCommand(URI uri);

    /**
     * Constructs a command line array used to reveal the given file in the system's file explorer.
     *
     * @param file the file to reveal.
     * @return an array of command-line arguments suitable for {@link ProcessBuilder}.
     */
    abstract String[] createFileBrowseCommand(File file);


    /**
     * Converts a {@link KeyEvent} into a normalized version compatible with Windows-style key mappings.
     * <p>This allows for consistent modifier key detection across platforms (e.g., treating Cmd as Ctrl on macOS).</p>
     *
     * @param keyEvent the original key event.
     * @return a normalized key event.
     */
    public abstract KeyEvent unifyKeyEvent(KeyEvent keyEvent);


    /**
     * Converts a {@link MouseEvent} into a normalized version compatible with Windows-style modifier mappings.
     * <p>This allows for consistent behavior of modifier flags across platforms (e.g., Ctrl vs Cmd).</p>
     *
     * @param mouseEvent the original mouse event.
     * @return a normalized mouse event.
     */
    public abstract MouseEvent unifyMouseEvent(MouseEvent mouseEvent);


    /**
     * Returns the location where application configuration files should be stored,
     * following OS-specific conventions.
     *
     * @param folderName the name of the configuration folder.
     * @return a {@link File} representing the configuration directory.
     */
    public abstract File createAppConfigDir(String folderName);
}
