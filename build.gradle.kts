plugins {
    java
    application
    pmd
    id("com.github.ben-manes.versions") version "0.51.0"
    id("com.diffplug.spotless") version "7.0.2"
    id("org.graalvm.buildtools.native") version "0.10.3"
}

group = "chess"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.williamcallahan:tui4j:0.3.3") {
        exclude(group = "org.jline", module = "jline-terminal-jni")
        exclude(group = "org.jline", module = "jline-native")
    }
    implementation("org.jline:jline-terminal-jna:3.26.1")
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
    format("misc") {
        target("*.md", "scripts/**/*.sh")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("chess")
            mainClass.set("chess.ChessApp")
            fallback.set(false)
            verbose.set(true)

            buildArgs.addAll(
                // JLine native (Windows JNI stubs) — defer to runtime where JLine handles failure
                "--initialize-at-run-time=org.jline.nativ",
                // JLine utils safe for build-time init
                "--initialize-at-build-time=org.jline.utils",
                // JNA needs runtime initialization
                "--initialize-at-run-time=com.sun.jna",
                // JLine terminal providers need runtime init
                "--initialize-at-run-time=org.jline.terminal.impl.jna",
                "--initialize-at-run-time=org.jline.terminal.impl.exec",
                // Report stack traces for debugging
                "-H:+ReportExceptionStackTraces",
                // Include all charsets
                "-H:+AddAllCharsets",
            )
        }
    }
}

// Convenience: run the native binary after building
tasks.register<Exec>("runNative") {
    dependsOn("nativeCompile")
    workingDir = projectDir
    commandLine("${layout.buildDirectory.get()}/native/nativeCompile/chess")
}
