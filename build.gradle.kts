plugins {
    java
    application
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
    implementation("com.williamcallahan:tui4j:0.3.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

application {
    mainClass.set("chess.ChessApp")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    // TUI apps need a real TTY — gradle's JavaExec doesn't provide raw mode.
    // Use `./gradlew installDist && build/install/console-chess/bin/console-chess` instead.
    doFirst {
        logger.warn("For arrow keys to work, run: ./build/install/console-chess/bin/console-chess")
    }
}

tasks.register<Exec>("play") {
    dependsOn("installDist")
    workingDir = projectDir
    commandLine("${buildDir}/install/${rootProject.name}/bin/${rootProject.name}")
}
