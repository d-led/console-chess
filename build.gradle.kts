plugins {
    java
    application
    pmd
    // Reports available dependency, plugin and Gradle updates; driven by
    // scripts/update-dependencies.sh. 0.55.0+ (the io.github.ben-manes namespace) is
    // required on Gradle 9, which removed the LenientConfiguration API used before it.
    id("io.github.ben-manes.versions") version "0.64.0"
    id("com.diffplug.spotless") version "8.10.2"
    id("org.graalvm.buildtools.native") version "1.1.13"
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
    // JLine native terminal providers: jni, backed by jline-native (per-platform
    // native libraries plus GraalVM native-image metadata), with jna as a fallback.
    // Held at the 3.x line even though 4.x is out: on 4.4.5 the app hangs after the
    // user quits. tui4j's input thread sits inside JLine's synchronized
    // NonBlockingInputStreamImpl.read() holding that monitor, and JLine 4's terminal
    // close() needs the same monitor (PumpThread.shutdown), so the process never
    // exits. 3.30.17 is the newest 3.x release of every module, jline-terminal-jna
    // included (JLine 4 dropped it). Revisit when tui4j supports JLine 4.
    // ** DO NOT UPDATE TO jline v4 **
    implementation("org.jline:jline-terminal-jni:3.30.17")
    implementation("org.jline:jline-native:3.30.17")
    implementation("org.jline:jline-terminal-jna:3.30.17")
    implementation("net.java.dev.jna:jna:5.19.1")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    // JUnit 6 no longer brings the launcher that Gradle's JUnit Platform support
    // needs on the test runtime classpath.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("com.approvaltests:approvaltests:31.0.0")
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
