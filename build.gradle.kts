import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.DEPRECATED_API_USAGES
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.INVALID_PLUGIN
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

project.group = "io.gitlab.arturbosch.detekt"
project.version = libs.versions.detekt.get()

repositories {
    mavenCentral()
}

plugins {
    id("org.jetbrains.intellij").version("1.7.0")
    id("com.github.ben-manes.versions").version("0.42.0")
    kotlin("jvm").version("1.7.10")
    id("com.github.breadmoirai.github-release").version("2.4.1")
}

dependencies {
    implementation(libs.detekt.api)
    implementation(libs.detekt.tooling)

    runtimeOnly(libs.detekt.core)
    runtimeOnly(libs.detekt.rules)
    runtimeOnly(libs.detekt.formatting)

    testImplementation(libs.detekt.testUtils)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter)
}

val jvmVersion = JavaVersion.VERSION_11
val currentJavaVersion = JavaVersion.current()
check(currentJavaVersion.isCompatibleWith(jvmVersion)) {
    "the current JVM ($currentJavaVersion) is incompatible with $jvmVersion"
}

java {
    sourceCompatibility = jvmVersion
    targetCompatibility = jvmVersion
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = jvmVersion.toString()
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        exceptionFormat = FULL
    }
}

tasks.publishPlugin {
    // This property can be configured via environment variable ORG_GRADLE_PROJECT_intellijPublishToken
    // See: https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
    token.set((findProperty("intellijPublishToken") as? String).orEmpty())
}

intellij {
    pluginName.set("Detekt IntelliJ Plugin")
    version.set("2022.2")
    updateSinceUntilBuild.set(false)
    plugins.set(listOf("IntelliLang", "Kotlin"))

}

tasks.runPluginVerifier {
    ideVersions.set(listOf("2020.3.4", "2021.1.3", "2021.2.4", "2021.3.3", "2022.1.4", "2022.2"))
    failureLevel.set(listOf(DEPRECATED_API_USAGES, INVALID_PLUGIN))
}

githubRelease {
    token((project.findProperty("github.token") as? String).orEmpty())
    owner.set("detekt")
    repo.set("detekt-intellij-plugin")
    targetCommitish.set("main")
    overwrite.set(true)
    dryRun.set(false)
    body {
        var changelog = project.file("changelog.md").readText()
        val sectionStart = "#### ${project.version}"
        changelog = changelog.substring(changelog.indexOf(sectionStart) + sectionStart.length)
        changelog = changelog.substring(0, changelog.indexOf("#### 1"))
        changelog.trim()
    }
    val distribution = project.buildDir
        .resolve("distributions/Detekt IntelliJ Plugin-${project.version}.zip")
    releaseAssets.setFrom(distribution)
}
