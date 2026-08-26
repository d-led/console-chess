plugins {
    java
    application
    pmd
    id("com.github.ben-manes.versions") version "0.51.0"
    id("com.diffplug.spotless") version "7.0.2"
    id("org.graalvm.buildtools.native") version "0.10.3"
}

group = "chess"

// Version resolution, in priority order:
//   1. an explicit `-Pversion=x.y.z` override (the release workflow passes it)
//   2. the semver git tag pointing at HEAD (vX.Y.Z -> X.Y.Z) for tag builds
//   3. a SNAPSHOT fallback for local/dev builds
val semverTag = Regex("v(\\d+\\.\\d+\\.\\d+)")

fun versionFromTag(): String? =
    runCatching {
        providers
            .exec {
                commandLine("git", "tag", "--points-at", "HEAD")
            }.standardOutput.asText
            .get()
            .lineSequence()
            .map { it.trim() }
            .firstNotNullOfOrNull { semverTag.matchEntire(it)?.groupValues?.get(1) }
    }.getOrNull()

version =
    (findProperty("version") as String?)
        ?.takeIf { it != "unspecified" && it.isNotBlank() }
        ?: versionFromTag()
        ?: "0.0.0-SNAPSHOT"

// Expose the resolved version to the app at runtime (ChessApp --version) by
// expanding the src/main/resources/version.properties template at build time.
tasks.processResources {
    filesMatching("version.properties") {
        expand(mapOf("version" to version))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.williamcallahan:tui4j:0.3.3")
    // JLine native terminal providers. jline-terminal-jni (backed by
    // jline-native, which bundles per-platform native libraries and GraalVM
    // native-image metadata) is what JLine itself recommends for native image;
    // jline-terminal-jna remains as a fallback. JLine picks providers in the
    // order ffm, jni, jna, exec (see TerminalBuilder.getProviders()).
    // 3.27.0+ is required on Windows: earlier jline-native builds stored the
    // 64-bit INVALID_HANDLE_VALUE as a 32-bit int, so GetConsoleMode() always
    // failed and every build fell back to a dumb terminal (jline3#1012).
    implementation("org.jline:jline-terminal-jni:3.27.1")
    implementation("org.jline:jline-native:3.27.1")
    implementation("org.jline:jline-terminal-jna:3.27.1")
    implementation("net.java.dev.jna:jna:5.14.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.approvaltests:approvaltests:24.5.0")
}

application {
    mainClass.set("chess.ChessApp")
}

tasks.test {
    useJUnitPlatform()
}

pmd {
    toolVersion = "7.9.0"
    isConsoleOutput = true
    ruleSets = listOf()
    ruleSetFiles = files("config/pmd/rules.xml")
}

spotless {
    java {
        googleJavaFormat()
        target("src/**/*.java")
        targetExclude("src/test/**/*.txt")
    }
    kotlinGradle {
        ktlint()
        target("*.gradle.kts")
    }
    format("misc") {
        target("*.md", "scripts/**/*.sh")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("console-chess")
            mainClass.set("chess.ChessApp")
            fallback.set(false)
            verbose.set(true)

            buildArgs.addAll(
                // JDK 24+ restricts System::load/loadLibrary. JLine's JNI provider
                // (JLineNativeLoader) and JNA both call these, so native access must
                // be enabled explicitly or the terminal providers cannot load.
                "--enable-native-access=ALL-UNNAMED",
                // JLine native (Windows JNI stubs) — defer to runtime where JLine handles failure
                "--initialize-at-run-time=org.jline.nativ",
                // JLine utils safe for build-time init
                "--initialize-at-build-time=org.jline.utils",
                // JNA needs runtime initialization
                "--initialize-at-run-time=com.sun.jna",
                // JLine terminal providers need runtime init
                "--initialize-at-run-time=org.jline.terminal.impl.jna",
                "--initialize-at-run-time=org.jline.terminal.impl.jni",
                "--initialize-at-run-time=org.jline.terminal.impl.exec",
                // ICU4J (via tui4j) loads BreakIteratorFactory reflectively and
                // reads its compiled break-rule data from resources at runtime.
                // Reachability metadata is auto-discovered from
                // src/main/resources/META-INF/native-image/chess/console-chess/.
                "--initialize-at-run-time=com.ibm.icu",
                // Report stack traces for debugging
                "-H:+ReportExceptionStackTraces",
                // Include all charsets
                "-H:+AddAllCharsets",
            )

            // Embed the Windows icon resource into the executable. CI compiles
            // docs/img/console-chess.rc to console-chess.res before invoking
            // nativeCompile; skipped when the file is absent (local builds).
            val iconRes = layout.projectDirectory.file("docs/img/console-chess.res").asFile
            if (iconRes.exists()) {
                buildArgs.add("-H:NativeLinkerOption=${iconRes.absolutePath}")
            }
        }
    }
}

// Convenience: run the native binary after building
tasks.register<Exec>("runNative") {
    dependsOn("nativeCompile")
    workingDir = projectDir
    commandLine("${layout.buildDirectory.get()}/native/nativeCompile/console-chess")
}
