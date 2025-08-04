# 🔐 Mango-Utils

**Mango-Utils** is a free, open-source collection of lightweight Java utility classes—ranging from simple I/O helpers to secure password hashing and configurable log management.

---

## 🔽 Installation

### 📦 Using the JAR

To use Mango-Utils as a standalone dependency:

1. Clone the repository locally.
2. Build the project using your preferred method (`mvn package`, `gradle build`, etc.).
3. Add the generated JAR file to your classpath.

---

### Ⓜ️ Maven

Add the JitPack repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then add the dependency:

```xml
<dependency>
    <groupId>com.github.redstonemango</groupId>
    <artifactId>mango-utils</artifactId>
    <version>master-SNAPSHOT</version>
</dependency>
```

---

### 🐘 Gradle

In your `settings.gradle` (if using version catalog or dependency management):

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Then in your `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.redstonemango:mango-utils:master-SNAPSHOT'
}
```

---

### ☕ Direct Source Copy

You may also directly copy specific utility classes into your own project, as long as you:

1. Keep the copyright notice at the top of the file.
2. Follow the terms of the [MIT License](https://github.com/RedStoneMango/mango-utils/blob/main/LICENSE).

---

## 📖 Available Utilities

| Class              | Description                                                                                                                                                       |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MangoIO`          | Handy I/O utilities like recursive directory deletion.                                                                                                            |
| `SemanticVersion`  | Compare and sort semantic versions (e.g., `1.2.0` vs `1.10.0`). Useful for dependency versioning.                                                                 |
| `Hasher`           | Password hashing and verification using PBKDF2 with HMAC-SHA-256.                                                                                                 |
| `CypherEncryption` | Easy AES-GCM encryption/decryption with password-based key derivation (PBKDF2).                                                                                   |
| `CliArg`           | Parses command-line arguments (flags, keys, values, etc.) from `String[]` or raw input strings.                                                                   |
| `LogManager`       | Customizable logging utility that redirects system output, styles console messages, and manages log files.                                                        |
| `OperatingSystem`  | A safe and cross-platform alternative to `java.awt.Desktop`'s open and browse methods. Also including OS detection, config path helpers, and event normalization. |

---

## ✅ Why Use Mango-Utils?

- 🛠️ Simple and modular design
- 📦 Easy integration with Maven and Gradle
- 🔐 Includes security utilities like password hashing and AES encryption
- 💡 Utility classes that fill common gaps in the Java standard library
- 🔄 Actively maintained and tested across real-world projects

---

## 📎 License

Mango-Utils is licensed under the [MIT License](https://github.com/RedStoneMango/mango-utils/blob/main/LICENSE).  
Feel free to use, modify, and redistribute it under the terms outlined there.

---

### 💬 Feedback & Contributions

Suggestions, issues, and pull requests are welcome! Feel free to open a discussion or submit a PR to contribute.
