import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.DEPRECATED_API_USAGES
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.INVALID_PLUGIN

project.group = "io.gitlab.arturbosch.detekt"
project.version = libs.versions.detektIJ.get()

repositories {
    mavenCentral()
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij)
    alias(libs.plugins.versions)
    alias(libs.plugins.github.release)
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

kotlin {
    jvmToolchain(11)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.publishPlugin {
    // This property can be configured via environment variable ORG_GRADLE_PROJECT_intellijPublishToken
    // See: https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
    token.set((findProperty("intellijPublishToken") as? String).orEmpty())
    // https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html#specifying-a-release-channel
    // "-beta" is used for pre-releases and https://plugins.jetbrains.com/plugins/beta/list as plugin repository.
    channels.set(listOf(project.version.toString().split('-').getOrElse(1) { "default" }.split('.').first()))
}

intellij {
    pluginName.set("Detekt IntelliJ Plugin")
    version.set("2022.1.4")
    updateSinceUntilBuild.set(false)
    plugins.set(listOf("IntelliLang", "Kotlin"))
}

tasks.runPluginVerifier {
    ideVersions.set(listOf("2022.1.4", "2022.2.4", "2022.3.2"))
    failureLevel.set(listOf(DEPRECATED_API_USAGES, INVALID_PLUGIN))
}

tasks.buildSearchableOptions {
    enabled = false
}

tasks.jarSearchableOptions {
    enabled = false
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
