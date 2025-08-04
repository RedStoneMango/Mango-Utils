/*
 * Copyright (c) 2025 RedStoneMango
 *
 * This file is licensed under the MIT License.
 * You may use, copy, modify, and distribute this file under the terms of the MIT License.
 * See the LICENSE file or https://opensource.org/licenses/MIT for full text.
 */

package io.github.redstonemango.mangoutils;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Represents a semantic version following the <a href="https://semver.org/">Semantic Versioning</a> specification.
 * Supports parsing, comparison, and validation of versions in the format MAJOR.MINOR.PATCH[-PRERELEASE].
 * <p>
 * Examples of valid versions:
 * <ul>
 *   <li>1.0.0</li>
 *   <li>2.1.5-alpha</li>
 *   <li>3.2.4-beta-1</li>
 *   <li>3.2.4-rc-3</li>
 * </ul>
 * </p>
 *
 * @author RedStoneMango
 */
public class SemanticVersion implements Comparable<SemanticVersion>  {
    private int MAJOR;
    private int MINOR;
    private int PATCH;
    private String preRelease; // new field

    private static final Pattern FULL_SEMVER_PATTERN = Pattern.compile(
            "^\\d+\\.\\d+\\.\\d+(-((alpha|beta|rc)(-\\d+)?))?$"
    );
    private static final Pattern PARTIAL_SEMVER_PATTERN = Pattern.compile(
            "^\\d+(\\.\\d+)?$"
    );

    /**
     * Constructs a semantic version with the specified major, minor, and patch versions.
     *
     * @param major the major version number
     * @param minor the minor version number
     * @param patch the patch version number
     */
    public SemanticVersion(int major, int minor, int patch) {
        this(major, minor, patch, null);
    }
    /**
     * Constructs a semantic version with the specified major, minor, patch versions and pre-release label.
     *
     * @param major      the major version number
     * @param minor      the minor version number
     * @param patch      the patch version number
     * @param preRelease the pre-release label (e.g., "alpha", "beta-1"), or {@code null} for a release version
     */
    public SemanticVersion(int major, int minor, int patch, String preRelease) {
        this.MAJOR = major;
        this.MINOR = minor;
        this.PATCH = patch;
        this.preRelease = preRelease;
    }
    /**
     * Parses a semantic version from a string.
     * Supports full syntax (e.g., "1.2.3", "1.0.0-alpha") and partial syntax (e.g., "1.2").
     *
     * @param semVer the version string to parse
     * @return a {@code SemanticVersion} instance
     * @throws IllegalArgumentException if the string does not represent a valid semantic version
     */
    public static SemanticVersion fromString(String semVer) {
        if (isValidSemVer(semVer)) {
            String[] mainAndPre = semVer.split("-", 2);
            String[] parts = mainAndPre[0].split("\\.");

            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);

            String preRelease = mainAndPre.length > 1 ? mainAndPre[1] : null;

            return new SemanticVersion(major, minor, patch, preRelease);
        } else if (isValidSemVer(semVer, false)) {
            String[] parts = semVer.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return new SemanticVersion(major, minor, 0);
        }

        throw new IllegalArgumentException("Invalid SemVer format");
    }

    /**
     * Validates whether a string is a syntactically correct semantic version (full syntax only).
     *
     * @param semVerString the version string to validate
     * @return {@code true} if the string is valid, {@code false} otherwise
     */
    public static boolean isValidSemVer(String semVerString) {
        return isValidSemVer(semVerString, true);
    }

    /**
     * Validates whether a string is a syntactically correct semantic version.
     * Can allow partial syntax (e.g., "1", "1.2") if {@code fullSyntaxesRequired} is false.
     *
     * @param semVerString         the version string to validate
     * @param fullSyntaxesRequired whether full syntax (MAJOR.MINOR.PATCH) is required
     * @return {@code true} if valid, {@code false} otherwise
     */
    public static boolean isValidSemVer(String semVerString, boolean fullSyntaxesRequired) {
        if (FULL_SEMVER_PATTERN.matcher(semVerString).matches()) return true;
        if (!fullSyntaxesRequired) {
            return PARTIAL_SEMVER_PATTERN.matcher(semVerString).matches();
        }
        return false;
    }

    /**
     * Determines whether this version is newer than the specified version.
     *
     * @param other the version to compare against
     * @return {@code true} if this version is newer, {@code false} otherwise
     */
    public boolean isNewerThan(SemanticVersion other) {
        return compare(other) > 0;
    }

    /**
     * Determines whether this version is older than the specified version.
     *
     * @param other the version to compare against
     * @return {@code true} if this version is older, {@code false} otherwise
     */
    public boolean isOlderThan(SemanticVersion other) {
        return compare(other) < 0;
    }

    /**
     * Compares this version to another version.
     *
     * @param other the other {@code SemanticVersion}
     * @return a negative integer, zero, or a positive integer as this version is less than,
     *         equal to, or greater than the specified version
     */
    public int compare(SemanticVersion other) {
        if (this.MAJOR != other.MAJOR) {
            return Integer.compare(this.MAJOR, other.MAJOR);
        }
        if (this.MINOR != other.MINOR) {
            return Integer.compare(this.MINOR, other.MINOR);
        }
        if (this.PATCH != other.PATCH) {
            return Integer.compare(this.PATCH, other.PATCH);
        }

        if (this.preRelease == null && other.preRelease == null) {
            return 0;
        }
        if (this.preRelease == null) {
            return 1;
        }
        if (other.preRelease == null) {
            return -1;
        }

        String[] thisParts = this.preRelease.split("-");
        String[] otherParts = other.preRelease.split("-");

        int nameCompare = thisParts[0].compareTo(otherParts[0]);
        if (nameCompare != 0) {
            return nameCompare;
        }

        if (thisParts.length > 1 && otherParts.length > 1) {
            try {
                int thisNum = Integer.parseInt(thisParts[1]);
                int otherNum = Integer.parseInt(otherParts[1]);
                return Integer.compare(thisNum, otherNum);
            } catch (NumberFormatException ignored) {
                return thisParts[1].compareTo(otherParts[1]);
            }
        }

        return Integer.compare(thisParts.length, otherParts.length);
    }

    /**
     * Compares this version to another version.
     *
     * @param other the other {@code SemanticVersion}
     * @return a negative integer, zero, or a positive integer as this version is less than,
     *         equal to, or greater than the specified version
     */
    @Override
    public int compareTo(@NotNull SemanticVersion other) {
        return compare(other);
    }

    /**
     * Checks if this version is equal to another object.
     *
     * @param obj the object to compare
     * @return {@code true} if the other object is a {@code SemanticVersion} with the same value, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SemanticVersion semVer) {
            return compare(semVer) == 0;
        }
        return false;
    }


    /**
     * Returns the string representation of this semantic version.
     *
     * @return the semantic version string (e.g., "1.0.0", "2.1.0-beta-1")
     */
    @Override
    public String toString() {
        return MAJOR + "." + MINOR + "." + PATCH + (preRelease != null ? "-" + preRelease : "");
    }

    /**
     * Returns the major version number.
     *
     * @return the major version
     */
    public int getMajor() {
        return MAJOR;
    }

    /**
     * Returns the minor version number.
     *
     * @return the minor version
     */
    public int getMinor() {
        return MINOR;
    }

    /**
     * Returns the patch version number.
     *
     * @return the patch version
     */
    public int getPatch() {
        return PATCH;
    }

    /**
     * Returns the pre-release label.
     *
     * @return the pre-release label, or {@code null} if none
     */
    public String getPreRelease() {
        return preRelease;
    }
    /**
     * Sets the major version number.
     *
     * @param major the new major version
     */
    public void setMajor(int major) {
        MAJOR = major;
    }
    /**
     * Sets the minor version number.
     *
     * @param minor the new minor version
     */
    public void setMinor(int minor) {
        MINOR = minor;
    }
    /**
     * Sets the patch version number.
     *
     * @param patch the new patch version
     */
    public void setPatch(int patch) {
        PATCH = patch;
    }
    /**
     * Sets the pre-release label.
     *
     * @param preRelease the new pre-release label (e.g., "rc-1"), or {@code null} for a release version
     */
    public void setPreRelease(String preRelease) {
        if (preRelease == null || !FULL_SEMVER_PATTERN.matcher("0.0.0-" + preRelease).matches()) {
            throw new IllegalArgumentException("Invalid pre-release format");
        }
        this.preRelease = preRelease;
    }
}
