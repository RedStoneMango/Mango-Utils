/*
 * Copyright (c) 2025 RedStoneMango
 *
 * This file is licensed under the MIT License.
 * You may use, copy, modify, and distribute this file under the terms of the MIT License.
 * See the LICENSE file or https://opensource.org/licenses/MIT for full text.
 */

package io.github.redstonemango.mangoutils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Configurable class that fetches the system's {@linkplain System#out out} and {@linkplain System#err error} streams and copies them into a log file, also providing styled prefixes for the console out and error streams.<br>
 * The log files are created once per day and will be compressed to a <code>.gz</code> on application startup, provided their last change is at least one day old.
 * The main usage of this class should look like the following:
 * <pre>
 *  {@code
 *  public static void main(String[] args) {
 *      // Configurations have to be done before starting the manager, otherwise an exception will be thrown.
 *      DailyLogManager.logDir(Paths.get("daily_logs"));
 *
 *      // Start the manager. This method will initialize all streams and handle all other events accordingly.
 *      DailyLogManager.start();
 *  }
 *  }
 * </pre>
 * @author RedStoneMango
 */
public class LogManager {

    /**
     * Internal {@link ExecutorService} for running background tasks asynchronously. This is used for compressing old logs during application startup or cleaning old ones.
     */
    private static final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("LogManager-Background-Thread");
        return t;
    });
    /**
     * Internal {@link PrintStream} representing the {@link System#out system's output stream} from the point in time, this class was first referenced.<br>
     * In most cases, this will be the default console stream, but it might also refer to any other stream, depending on the user's implementation.
     */
    private static final PrintStream consoleOut = System.out;
    /**
     * Internal {@link PrintStream} representing the {@link System#err system's error stream} from the point in time, this class was first referenced.<br>
     * In most cases, this will be the default console stream, but it might also refer to any other stream, depending on the user's implementation.
     */
    private static final PrintStream consoleErr = System.err;
    /**
     * Internal boolean stating whether the log manager has been started. If this value is <code>true</code>, the {@linkplain #checkStartedAndExcept() "checkStartedAndExcept" method} will throw an {@link IllegalStateException}.
     */
    private static boolean started = false;
    /**
     * {@link Function} supplying the {@link String} to be used as line prefix in the log file and <i>(if {@link #maintainConsoleStream} is <code>true</code>)</i> the console. The function's first argument states whether the prefix will be used to mark an error inside the error stream (<code>true</code>, if it refers to the error stream, <code>false</code> otherwise). The second argument states whether the prefix is used for the console or the log file stream (<code>true</code>, if it is used in the console, <code>false</code> otherwise), allowing the user to implement formatting using <a href="https://en.wikipedia.org/wiki/ANSI_escape_code">ANSI escape codes</a> in the console view<br>
     * By default, this is
     * <pre>
     *  {@code
     *  (isErrorStream, isConsole) ->
     *             String.format("%s[%-8s/%s%5s%s]:%s ",
     *                     isConsole ? "\033[1;35m" : "",
     *                     LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
     *                     isConsole ? "\033[" + (isErrorStream ? "31m" : "34m") : "",
     *                     isErrorStream ? "Error" : "Info",
     *                     isConsole ? "\033[1;35m" : "",
     *                     isConsole ? isErrorStream ? "\033[0;31m" : "\033[0m" : "")
     *  }
     * </pre>
     * resulting in a prefix like <code>[16:48:24/ Info]: </code> or <code>[21:52:38/Error]: </code> that <i>(if displayed in the console)</i> formats it as a bold magenta text, highlighting the "Info" or "Error" snippets in either blue or red and coloring the whole error message in a red style.
     */
    private static BiFunction<Boolean, Boolean, String> logPrefixFunction = (isErrorStream, isConsole) ->
            String.format("%s[%-8s/%s%5s%s]:%s ",
                    isConsole ? "\033[1;35m" : "",
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    isConsole ? "\033[" + (isErrorStream ? "31m" : "34m") : "",
                    isErrorStream ? "Error" : "Info",
                    isConsole ? "\033[1;35m" : "",
                    isConsole ? isErrorStream ? "\033[0;31m" : "\033[0m" : "");
    /**
     * {@link Function} supplying the {@link String} to be used as lifetime comment in the log file. Such a comment is added to the file when the application starts up or shuts down. The function's argument states whether the comment is about a startup (true, if the app is currently starting, false otherwise)<br>
     * By default, this is
     * <pre>
     *  {@code
     *  isStartup -> (isStartup ? "\n" : "") + "\n--- Application " + (isStartup ? "startup" : "shutdown") + " at " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "---\n"
     *  }
     * </pre>
     * resulting in a comment like <code>--- Application startup at 15:13:42---</code> or <code>--- Application shutdown at 13:46:32---</code>
     */
    private static Function<Boolean, String> lifetimeCommentFunction = isStartup -> (isStartup ? "\n" : "") + "\n--- Application " + (isStartup ? "startup" : "shutdown") + " at " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "---\n";
    /**
     * {@link Function} supplying the {@link String} to be used name for the log file <i>(without .log extension)</i>. The function's argument represents the {@link LocalDate} the log file is created for<br>
     * By default, this is
     * <pre>
     *  {@code
     *  date -> "log_" + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
     *  }
     * </pre>
     * resulting in a name like <code>log_2025-08-21</code> (which will be completed to <code>log_2025-08-21<b>.log</b></code> automatically).
     */
    private static Function<LocalDate, String> logFileNameFunction = date -> "log_" + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    /**
     * {@link Function} supplying the {@link String} to be used for the log file header. This header is a text added to the beginning of every log file and should be used to inform the user about the content and use of the log file. The function's argument represents the {@link LocalDate} the log file is created for<br>
     * By default, this is
     * <pre>
     *  {@code
     *  date -> "LOG FILE FOR " + date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) + "\n-----------------------"
     *  }
     * </pre>
     * resulting in a header like <code>Log file for 08/21/2025</code> <i>(to represent the 21st of August 2025)</i> with a separator consisting of hyphens below it.
     */
    private static Function<LocalDate, String> logFileHeaderFunction = date -> "LOG FILE FOR " + date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) + "\n-----------------------";
    /**
     * The {@link Path} to store the log files in.<br>
     * This is the path, used to save log files, write to them, compress old ones and clean up expired files
     */
    private static Path logDir = Paths.get("logs");
    /**
     * The amount of day to retain log files. If a file has not been edited for this number of days <i>(or more)</i>, the log file will automatically be deleted to save storage.<br>
     * <br>
     * <b>Note, that this value does not refer to a day by the modification date, but by a timespan of 24 hours after the last modification happened.</b>
     */
    private static int ARCHIVE_RETENTION_DAYS = 7;
    /**
     * Whether the old {@linkplain System#out out} and {@linkplain System#err error} streams shall be maintained and the printed data copied to them.<br>
     * If this is <code>true</code>, methods like <code>System.out.println</code> will still print something to the console and copy the text to the log file only.<br>
     * If this is <code>false</code>, <code>System.out.println</code> will not print something to the console, but to the log file only.<br>
     * <br>
     * Although, this documentation talks about the console stream, it refers to the streams set before referencing this class for the fist time. In most cases, this will be the stream sent to the console, but it may differ, based on the user's implementation.
     */
    private static boolean maintainConsoleStream = true;
    /**
     * States whether the printing, compression and clearing of log files is temporally disabled. If this value is <code>true</code>, no log file related actions will be run<br>
     * This is useful for long debugging sessions that would cause unnecessary logs to be created and managed while debugging the application while still applying prefixes to the console stream <i>(if enabled using {@link #maintainConsoleStream})</i>.<br>
     * <br>
     * Note that this value is not meant to be set to <code>true</code> in a release build of the application. On startup <i>({@link #start()})</i> the log manager will automatically warn you if this value is set to <code>true</code>. Although it would not cause any damage to the application, the use of this feature defeats the main purpose of the log manager library.
     */
    private static boolean disableLogFiles = false;
    /**
     * The number of milliseconds to hold the {@linkplain Runtime#addShutdownHook(Thread) shutdown hook} thread before disabling the log manager.<br>
     * Increasing this value might be useful if you have more shutdown threads inside your application, and you want the log manager to be active while their execution.<br>
     * <br>
     * Keep in mind that this delay will keep your application alive. The longer the delay is, the longer it will take your application to terminate.<br>
     * If an {@link InterruptedException} is thrown while waiting the specified number of milliseconds, the exception will be caught silently, immediately disabling the log manager, but not causing any problems.
     */
    private static long shutdownDelay = 0;
    /**
     * The abstract file {@link Path} to today's log file.<br>
     * This value is initialized when the log manager starts and cannot be referenced before that point in time.
     */
    private static @Nullable Path todayLog = null;


    /**
     * Starts the log manager. This method initializes all streams required to write to the log file, sets up a hook for shutting down the manger on {@linkplain Runtime#addShutdownHook(Thread) VM shutdown} and initiates the compression and removal of older files.<br>
     * If the log manager has already been started, this method throws an {@link IllegalStateException}
     * @throws IllegalStateException If the log manager has already been started.
     */
    public static void start() throws IllegalStateException {
        checkStartedAndExcept();
        started = true;

        try {
            OutputStream syncLogFileOut = new OutputStream() {@Override public synchronized void write(int b) {}};

            if (!disableLogFiles) {
                Files.createDirectories(logDir);
                LocalDate today = LocalDate.now();

                backgroundExecutor.submit(() -> {
                    try {
                        compressOldLogs(today);
                    } catch (IOException e) {
                        System.err.println("Error during log compression: " + e.getMessage());
                    }
                });

                backgroundExecutor.submit(() -> {
                    try {
                        cleanupOldArchives();
                    } catch (IOException e) {
                        System.err.println("Error during archive cleanup: " + e.getMessage());
                    }
                });

                todayLog = logDir.resolve(logFileNameFunction.apply(LocalDate.now()) + ".log");
                OutputStream logFileOut = new BufferedOutputStream(new FileOutputStream(todayLog.toFile(), true));
                syncLogFileOut = new OutputStream() {
                    @Override
                    public synchronized void write(int b) throws IOException {
                        logFileOut.write(b);
                    }
                    @Override
                    public synchronized void write(byte @NotNull [] b, int off, int len) throws IOException {
                        logFileOut.write(b, off, len);
                    }
                    @Override
                    public synchronized void flush() throws IOException {
                        logFileOut.flush();
                    }
                    @Override
                    public synchronized void close() throws IOException {
                        logFileOut.close();
                    }
                };
                if (!Files.exists(todayLog)) {
                    Files.createDirectories(todayLog.getParent());
                    Files.createFile(todayLog);
                }

                try (FileWriter writer = new FileWriter(todayLog.toFile(), true)) {
                    if (!new Scanner(todayLog.toFile()).useDelimiter("\\Z").hasNext()) { // Check whether the log file is empty
                        writer.write(logFileHeaderFunction.apply(LocalDate.now()));
                    }
                    writer.write(lifetimeCommentFunction.apply(true));
                }

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        Thread.sleep(shutdownDelay); // Wait a bit to let possible other shutdown hooks (created by the application) finish their possible debugging before writing the lifetime comment
                    } catch (InterruptedException ignored) {}

                    try (FileWriter writer = new FileWriter(todayLog.toFile(), true)) {
                        writer.write(lifetimeCommentFunction.apply(false));
                    }
                    catch (IOException e) {
                        e.printStackTrace(consoleErr);
                    }

                    backgroundExecutor.shutdown();
                    try {
                        if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                            backgroundExecutor.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        backgroundExecutor.shutdownNow();
                    }
                }));

            }

            OutputStream empty = new OutputStream() {@Override public void write(int b) {}};
            PrefixedTeePrintStream out = new PrefixedTeePrintStream(maintainConsoleStream ? consoleOut : empty, syncLogFileOut, false);
            PrefixedTeePrintStream err = new PrefixedTeePrintStream(maintainConsoleStream ? consoleErr : empty, syncLogFileOut, true);
            System.setOut(out);
            System.setErr(err);
            empty.close();
        } catch (IOException e) {
            e.printStackTrace(consoleErr);
        }
    }

    /**
     * Internal helper method for finding old logs and initiating their compression using {@link #compressAndDeleteLog(Path)}.<br>
     * The method walks through every <code>.log</code> file in the {@link #logDir()}, checks for it's last modification to have happened at least the day before and then starts the compression of these files.
     * @param today The current {@link LocalDate}, obtained using {@link LocalDate#now()} in {@link #start()}. Used to identify whether the last modification happened at least the day before.
     * @throws IOException If an I/O error occurs when opening the {@link #logDir() log directory}.
     */
    private static void compressOldLogs(LocalDate today) throws IOException {
        try (Stream<Path> files = Files.list(logDir)) {
            files.filter(path -> path.toString().endsWith(".log"))
                    .filter(path -> {
                        FileTime lastModifiedTime;
                        try {
                            lastModifiedTime = Files.getLastModifiedTime(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        LocalDate fileDate = lastModifiedTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        return fileDate.isBefore(today);
                    })
                    .forEach(path -> {
                        try {
                            compressAndDeleteLog(path);
                        } catch (IOException e) {
                            System.err.println("Failed to compress log: " + path);
                        }
                    });
        }
    }
    /**
     * Internal helper method for compressing a <code>.log</code> file using apache's common-compress library and later deleting the original log file.<br>
     * If the given {@link Path} does not point to a <code>.log</code> file, the method cancels itself gracefully without throwing an exception.
     * @param logFile The <code>.log</code> file to compress and delete.
     * @throws IOException If an I/O error occurs during the compression process.
     */
    private static void compressAndDeleteLog(Path logFile) throws IOException {
        String fileName = logFile.getFileName().toString();
        if (!fileName.endsWith(".log")) {
            return;
        }

        String baseName = fileName.substring(0, fileName.length() - ".log".length());
        Path gzipFile = logDir.resolve(baseName + ".gz");

        try (
                FileInputStream fis = new FileInputStream(logFile.toFile());
                FileOutputStream fos = new FileOutputStream(gzipFile.toFile());
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                GZIPOutputStream gzos = new GZIPOutputStream(bos)
        ) {
            fis.transferTo(gzos);
        }

        Files.deleteIfExists(logFile);
    }

    /**
     * Internal helper method for cleaning up old archives.<br>
     * This method walks all files in the {@link #logDir()}, checks for them to be a <code>.gz</code> file, checks whether the last modification happened before at least the amount of day specified in {@link #archiveRetentionDays()} and deletes them if this is the case.
     * @throws IOException If an I/O error occurs when opening the {@link #logDir() log directory}.
     */
    private static void cleanupOldArchives() throws IOException {
        try (Stream<Path> files = Files.list(logDir)) {
            files.filter(path -> path.toString().endsWith(".gz"))
                    .filter(path -> {
                        try {
                            FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                            return lastModifiedTime.toInstant().isBefore(Instant.now().minus(Duration.ofDays(ARCHIVE_RETENTION_DAYS)));
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete old archive: " + path);
                        }
                    });
        }
    }

    /**
     * Internal helper method that checks whether the log manager has already been started and throws an {@link IllegalStateException} if this is the case.
     * @throws IllegalStateException If the log manager has already been started.
     */
    private static void checkStartedAndExcept() throws IllegalStateException {
        if (started) throw new IllegalStateException("The log manager has already been started. No changes are applicable any more");
    }

    /**
     * Resolves the exact {@link Path} object used by the log manager to write to today's log file.<br>
     * This is possible only if the log manager has already started. Otherwise, an {@link IllegalStateException} will be thrown.
     * @return The exact {@link Path} object used for writing to the log file.
     * @throws IllegalStateException If the log manager has not been started yet.
     */
    public static Path resolveTodayLog() throws IllegalStateException {
        if (!started) throw new IllegalStateException("The log manager has not been started yet. Unable to resolve today's log file");
        return todayLog;
    }

    /**
     * Returns the value of {@link #logDir}.<br>
     * For more information on the value, refer to {@linkplain #logDir this documentation}.
     * @return The value of {@link #logDir}.
     */
    public static Path logDir() {
        return logDir;
    }
    /**
     * Sets the value of {@link #logDir}. Throws an {@link IllegalStateException} if the log manager has already been started and no changes are applicable.<br>
     * For more information on this value, refer to {@linkplain #logDir this documentation}.
     * @param logDir The new value for {@link #logDir}
     * @throws IllegalStateException If the log manager has already been started and no changes are applicable
     */
    public static void logDir(Path logDir) throws IllegalStateException {
        checkStartedAndExcept();
        LogManager.logDir = logDir;
    }

    /**
     * Returns the value of {@link #ARCHIVE_RETENTION_DAYS}.<br>
     * For more information on the value, refer to {@linkplain #ARCHIVE_RETENTION_DAYS this documentation}.
     * @return The value of {@link #ARCHIVE_RETENTION_DAYS}.
     */
    public static int archiveRetentionDays() {
        return ARCHIVE_RETENTION_DAYS;
    }
    /**
     * Sets the value of {@link #ARCHIVE_RETENTION_DAYS}. Throws an {@link IllegalStateException} if the log manager has already been started and no changes are applicable.<br>
     * For more information on this value, refer to {@linkplain #ARCHIVE_RETENTION_DAYS this documentation}.
     * @param days The new value for {@link #ARCHIVE_RETENTION_DAYS}
     * @throws IllegalStateException If the log manager has already been started and no changes are applicable
     */
    public static void archiveRetentionDays(int days) throws IllegalStateException {
        checkStartedAndExcept();
        ARCHIVE_RETENTION_DAYS = days;
    }

    /**
     * Returns the value of {@link #maintainConsoleStream}.<br>
     * For more information on the value, refer to {@linkplain #maintainConsoleStream this documentation}.
     * @return The value of {@link #maintainConsoleStream}.
     */
    public static boolean maintainConsoleStream() {
        return maintainConsoleStream;
    }
    /**
     * Sets the value of {@link #ARCHIVE_RETENTION_DAYS}. Throws an {@link IllegalStateException} if the log manager has already been started and no changes are applicable.<br>
     * For more information on this value, refer to {@linkplain #ARCHIVE_RETENTION_DAYS this documentation}.
     * @param maintain The new value for {@link #ARCHIVE_RETENTION_DAYS}
     * @throws IllegalStateException If the log manager has already been started and no changes are applicable
     */
    public static void maintainConsoleStream(boolean maintain) throws IllegalStateException {
        checkStartedAndExcept();
        maintainConsoleStream = maintain;
    }

    /**
     * Returns the value of {@link #logPrefixFunction}.<br>
     * For more information on the value, refer to {@linkplain #logPrefixFunction this documentation}.
     * @return The value of {@link #logPrefixFunction}.
     */
    public static BiFunction<Boolean, Boolean, String> logPrefixFunction() {
        return logPrefixFunction;
    }
    /**
     * Sets the value of {@link #logPrefixFunction}. Throws an {@link IllegalStateException} if the log manager has already been started and no changes are applicable.<br>
     * For more information on this value, refer to {@linkplain #logPrefixFunction this documentation}.
     * @param function The new value for {@link #logPrefixFunction}
     * @throws IllegalStateException If the log manager has already been started and no changes are applicable
     */
    public static void logPrefixFunction(BiFunction<Boolean, Boolean, String> function) throws IllegalStateException {
        checkStartedAndExcept();
        logPrefixFunction = function;
    }

    /**
     * Returns the value of {@link #lifetimeCommentFunction}.<br>
     * For more information on the value, refer to {@linkplain #lifetimeCommentFunction this documentation}.
     * @return The value of {@link #lifetimeCommentFunction}.
     */
    public static Function<Boolean, String> lifetimeCommentFunction() {
        return lifetimeCommentFunction;
    }
    /**
     * Sets the value of {@link #lifetimeCommentFunction}. Throws an {@link IllegalStateException} if the log manager has already been started and no changes are applicable.<br>
     * For more information on this value, refer to {@linkplain #lifetimeCommentFunction this documentation}.
     * @param function The new value for {@link #lifetimeCommentFunction}
     * @throws IllegalStateException If the log manager has already been started and no changes are applicable
     */
    public static void lifetimeCommentFunction(Function<Boolean, String> function) throws IllegalStateException {
        checkStartedAndExcept();
        lifetimeCommentFunction = function;
    }

    /**
     * Returns the value of {@link #logFileNameFunction}.<br>
     * For more information on the value, refer to {@linkplain #logFileNameFunction this documentation}.
     * @return The value of {@link #logFileNameFunction}.
     */
    public static Function<LocalDate, String> logFileNameFunction() {
        return logFileNameFunction;
    }
    /**
     * Sets the value of {@link #logFileNameFunction}. Throws an {@link IllegalStateException} if the log manager has already been started and no changes are applicable.<br>
     * For more information on this value, refer to {@linkplain #logFileNameFunction this documentation}.
     * @param function The new value for {@link #logPrefixFunction}
     * @throws IllegalStateException If the log manager has already been started and no changes are applicable
     */
    public static void logFileNameFunction(Function<LocalDate, String> function) throws IllegalStateException {
        checkStartedAndExcept();
        logFileNameFunction = function;
    }

    /**
     * Returns the value of {@link #logFileHeaderFunction}.<br>
     * For more information on the value, refer to {@linkplain #logFileHeaderFunction this documentation}.
     * @return The value of {@link #logFileHeaderFunction}.
     */
    public static Function<LocalDate, String> logFileHeaderFunction() {
        return logFileHeaderFunction;
    }
    /**
     * Sets the value of {@link #logFileHeaderFunction}. Throws an {@link IllegalStateException} if the log manager has already been started and no changes are applicable.<br>
     * For more information on this value, refer to {@linkplain #logFileHeaderFunction this documentation}.
     * @param function The new value for {@link #logPrefixFunction}
     * @throws IllegalStateException If the log manager has already been started and no changes are applicable
     */
    public static void logFileHeaderFunction(Function<LocalDate, String> function) throws IllegalStateException {
        checkStartedAndExcept();
        logFileHeaderFunction = function;
    }

    /**
     * Returns the value of {@link #disableLogFiles}.<br>
     * For more information on the value, refer to {@linkplain #disableLogFiles this documentation}.
     * @return The value of {@link #disableLogFiles}.
     */
    public static boolean disableLogFiles() {
        return disableLogFiles;
    }
    /**
     * Sets the value of {@link #disableLogFiles}. Throws an {@link IllegalStateException} if the log manager has already been started and no changes are applicable.<br>
     * For more information on this value, refer to {@linkplain #disableLogFiles this documentation}.
     * @param disable The new value for {@link #disableLogFiles}
     * @throws IllegalStateException If the log manager has already been started and no changes are applicable
     */
    public static void disableLogFiles(boolean disable) throws IllegalStateException {
        checkStartedAndExcept();
        disableLogFiles = disable;
    }

    /**
     * Returns the value of {@link #shutdownDelay}.<br>
     * For more information on the value, refer to {@linkplain #shutdownDelay this documentation}.
     * @return The value of {@link #shutdownDelay}.
     */
    public static long shutdownDelay() {
        return shutdownDelay;
    }
    /**
     * Sets the value of {@link #shutdownDelay}. Throws an {@link IllegalStateException} if the log manager has already been started and no changes are applicable.<br>
     * For more information on this value, refer to {@linkplain #shutdownDelay this documentation}.
     * @param millis The new value for {@link #shutdownDelay}
     * @throws IllegalStateException If the log manager has already been started and no changes are applicable
     */
    public static void shutdownDelay(long millis) {
        LogManager.shutdownDelay = millis;
    }

    /**
     * Implementation of a {@link PrintStream} allowing to print to two {@link OutputStream OutputStreams} using the same {@link PrintStream}.<br>
     * This class also contains an {@linkplain Override override} for the {@link #print(String)}, {@link #println(String)} and {@link #printf(String, Object...)} methods to append a prefix supplied by the {@link #logPrefixFunction} to every line.
     */
    public static class PrefixedTeePrintStream extends PrintStream {

        /**
         * The {@link OutputStream} that points to the log file.
         */
        private final OutputStream fileStream;
        /**
         * States whether this stream is used for printing errors.<br>
         * <code>true</code> if errors are printed using this stream, <code>false</code> otherwise.
         * @see #isErrorStream()
         */
        private final boolean errorStream;

        /**
         * Internal value stating whether this is the first call of a print function on this class.<br>
         * As soon as any form of data was printed using this instance, the value will become <code>false</code> and stay this way for the rest of this application's lifetime.
         */
        private boolean firstCall = true;
        /**
         * Internal value stating whether the next call of a print function will start a new line and therefore append a prefix to it.
         */
        private boolean newLineStart = true; // By default, we are at the beginning of a new line
        /**
         * Internal helper value storing the buffered {@link String} to be written inside the log file
         */
        private String fileText = "";
        /**
         * Internal lock object for synchronizing the stream when used concurrently
         */
        private final Object lock = new Object();

        /**
         * Constructor for this class.
         * @param consoleStream The {@link OutputStream} pointing to the console. This value will be passed into the superclass's constructor.
         * @param fileStream The {@link OutputStream} pointing to the log file. This value will be stored as {@link #fileStream}.
         * @param errorStream Boolean stating whether error will be printed using this stream. This value will be stored as {@link #errorStream}
         */
        public PrefixedTeePrintStream(@NotNull OutputStream consoleStream, OutputStream fileStream, boolean errorStream) {
            super(consoleStream);
            this.fileStream = fileStream;
            this.errorStream = errorStream;
        }

        // --- Hook Methods ---

        /**
         * Hook for the {@link #print(String)} and {@link #printf(String, Object...)} methods.<br>
         * This method handles the appending of prefixes, writes to the log file and prints these data to the console.<br>
         * Because of the logic inside the {@link PrintStream#println()} methods, every use of the {@link PrintStream#println()} methods will indirectly call this hook too, allowing us to collect all required logic inside this single method.
         * @param s The {@link String} to be printed. If this hook is called by one of the {@link #print(Object)} methods, this will be the method argument's string representation. If this was called by one of the {@link #printf(String, Object...)} methods, this will be the formatted string.
         */
        protected void printHook(String s) {
            synchronized (lock) {
                if (s == null) s = "null";
                String conPrefix = logPrefixFunction.apply(errorStream, true);
                String filePrefix = logPrefixFunction.apply(errorStream, false);

                if (newLineStart) {
                    super.print(conPrefix);
                    fileText = firstCall ? filePrefix : "\n" + filePrefix;
                    firstCall = false;
                }

                try {
                    String text = fileText + s.replace("\n", "\n" + filePrefix);
                    fileStream.write(text.getBytes(StandardCharsets.UTF_8));
                    fileStream.flush();
                } catch (IOException e) {
                    consoleErr.println("An unexpected exception was thrown while writing to today's log file: " + e.getMessage());
                    e.printStackTrace(consoleErr);
                }

                super.print(s.replace("\n", "\n" + conPrefix));
                newLineStart = false;
                fileText = "";
            }
        }

        /**
         * Hook for the {@link #println()} methods.<br>
         * This method detects the start of a new line and then calls {@link PrintStream#println(String)} what will (because of the logic inside {@link PrintStream#println(String)}) cause the {@link #printHook(String)} to trigger and handle the main logic.
         * @param s The {@link String} ot be printed. This is the string representation of the argument passed into the println method that called this hook.
         */
        protected void printlnHook(String s) {
            synchronized (lock) {
                super.println(s);
                newLineStart = true;
            }
        }

        /**
         * Returns the value of {@link #errorStream}.
         * @return The value of {@link #errorStream}.
         */
        public boolean isErrorStream() {
            return errorStream;
        }

        // --- Implementations for invoking hook methods ---

        /**
         * Implementation of {@link PrintStream#print(String)}. This method calls the {@link #printHook(String)} with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param s   The {@code String} to be printed
         */
        @Override
        public void print(String s) {
            printHook(s);
        }

        /**
         * Implementation of {@link PrintStream#print(boolean)}. This method calls the {@link #printHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param b   The {@code boolean} to be printed
         */
        @Override
        public void print(boolean b) {
            String s = String.valueOf(b);
            printHook(s);
        }

        /**
         * Implementation of {@link PrintStream#print(char)}. This method calls the {@link #printHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param c   The {@code char} to be printed
         */
        @Override
        public void print(char c) {
            String s = String.valueOf(c);
            printHook(s);
        }

        /**
         * Implementation of {@link PrintStream#print(int)}. This method calls the {@link #printHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param i   The {@code int} to be printed
         */
        @Override
        public void print(int i) {
            String s = String.valueOf(i);
            printHook(s);
        }

        /**
         * Implementation of {@link PrintStream#print(long)}. This method calls the {@link #printHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param l   The {@code long} to be printed
         */
        @Override
        public void print(long l) {
            String s = String.valueOf(l);
            printHook(s);
        }

        /**
         * Implementation of {@link PrintStream#print(float)}. This method calls the {@link #printHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param f   The {@code float} to be printed
         */
        @Override
        public void print(float f) {
            String s = String.valueOf(f);
            printHook(s);
        }

        /**
         * Implementation of {@link PrintStream#print(double)}. This method calls the {@link #printHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param d   The {@code double} to be printed
         */
        @Override
        public void print(double d) {
            String s = String.valueOf(d);
            printHook(s);
        }

        /**
         * Implementation of {@link PrintStream#print(char[])}. This method calls the {@link #printHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param s   The {@code char[]} to be printed
         */
        @Override
        public void print(char @NotNull [] s) {
            String str = new String(s);
            printHook(str);
        }

        /**
         * Implementation of {@link PrintStream#print(Object)}. This method calls the {@link #printHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param obj   The {@code Object} to be printed
         */
        @Override
        public void print(Object obj) {
            String s = String.valueOf(obj);
            printHook(s);
        }

        /**
         * Implementation of {@link PrintStream#println()}. This method calls the {@link #printlnHook(String)}
         * with an empty string as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         */
        @Override
        public void println() {
            printlnHook("");
        }

        /**
         * Implementation of {@link PrintStream#println(String)}. This method calls the {@link #printlnHook(String)}
         * with the string argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param s   The {@code String} to be printed with a newline
         */
        @Override
        public void println(String s) {
            printlnHook(s);
        }

        /**
         * Implementation of {@link PrintStream#println(boolean)}. This method calls the {@link #printlnHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param x   The {@code boolean} to be printed with a newline
         */
        @Override
        public void println(boolean x) {
            String s = String.valueOf(x);
            printlnHook(s);
        }

        /**
         * Implementation of {@link PrintStream#println(char)}. This method calls the {@link #printlnHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param x   The {@code char} to be printed with a newline
         */
        @Override
        public void println(char x) {
            String s = String.valueOf(x);
            printlnHook(s);
        }

        /**
         * Implementation of {@link PrintStream#println(int)}. This method calls the {@link #printlnHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param x   The {@code int} to be printed with a newline
         */
        @Override
        public void println(int x) {
            String s = String.valueOf(x);
            printlnHook(s);
        }

        /**
         * Implementation of {@link PrintStream#println(long)}. This method calls the {@link #printlnHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param x   The {@code long} to be printed with a newline
         */
        @Override
        public void println(long x) {
            String s = String.valueOf(x);
            printlnHook(s);
        }

        /**
         * Implementation of {@link PrintStream#println(float)}. This method calls the {@link #printlnHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param x   The {@code float} to be printed with a newline
         */
        @Override
        public void println(float x) {
            String s = String.valueOf(x);
            printlnHook(s);
        }

        /**
         * Implementation of {@link PrintStream#println(double)}. This method calls the {@link #printlnHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param x   The {@code double} to be printed with a newline
         */
        @Override
        public void println(double x) {
            String s = String.valueOf(x);
            printlnHook(s);
        }

        /**
         * Implementation of {@link PrintStream#println(char[])}. This method calls the {@link #printlnHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param x   The {@code char[]} to be printed with a newline
         */
        @Override
        public void println(char @NotNull [] x) {
            String s = new String(x);
            printlnHook(s);
        }

        /**
         * Implementation of {@link PrintStream#println(Object)}. This method calls the {@link #printlnHook(String)}
         * with the string representation of its argument as a parameter. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param x   The {@code Object} to be printed with a newline
         */
        @Override
        public void println(Object x) {
            String s = String.valueOf(x);
            printlnHook(s);
        }

        /**
         * Implementation of {@link PrintStream#printf(String, Object...)}. This method calls the {@link #printHook(String)}
         * with the formatted string. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param format   A format string
         * @param args     Arguments referenced by the format specifiers
         * @return This stream
         */
        @Override
        public PrintStream printf(@NotNull String format, Object... args) {
            String s = String.format(format, args);
            printHook(s);
            return this;
        }

        /**
         * Implementation of {@link PrintStream#printf(Locale, String, Object...)}. This method calls the {@link #printHook(String)}
         * with the formatted string using the specified locale. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param l        The {@code Locale} to apply during formatting
         * @param format   A format string
         * @param args     Arguments referenced by the format specifiers
         * @return This stream
         */
        @Override
        public PrintStream printf(Locale l, @NotNull String format, Object... args) {
            String s = String.format(l, format, args);
            printHook(s);
            return this;
        }

        /**
         * Implementation of {@link PrintStream#format(String, Object...)}. This method calls the {@link #printHook(String)}
         * with the formatted string. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param format   A format string
         * @param args     Arguments referenced by the format specifiers
         * @return This stream
         */
        @Override
        public PrintStream format(@NotNull String format, Object... args) {
            String s = String.format(format, args);
            printHook(s);
            return this;
        }

        /**
         * Implementation of {@link PrintStream#format(Locale, String, Object...)}. This method calls the {@link #printHook(String)}
         * with the formatted string using the specified locale. The base method's documentation is:<br>
         * "{@inheritDoc}"
         * @param l        The {@code Locale} to apply during formatting
         * @param format   A format string
         * @param args     Arguments referenced by the format specifiers
         * @return This stream
         */
        @Override
        public PrintStream format(Locale l, @NotNull String format, Object... args) {
            String s = String.format(l, format, args);
            printHook(s);
            return this;
        }
    }
}
