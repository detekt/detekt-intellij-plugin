import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellijPlatform)
    alias(libs.plugins.versions)
    alias(libs.plugins.github.release)
}

project.group = "io.gitlab.arturbosch.detekt"
project.version = libs.versions.detektIJ.get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    constraints {
        runtimeOnly(libs.slf4j.api) {
            because("transitive ktlint logging dependency (2.0.3) does not use the module classloader in ServiceLoader")
        }
    }

    implementation(libs.detekt.api) { exclude(group = "org.jetbrains.kotlinx") }
    implementation(libs.detekt.tooling) { exclude(group = "org.jetbrains.kotlinx") }

    runtimeOnly(libs.detekt.core) { exclude(group = "org.jetbrains.kotlinx") }
    runtimeOnly(libs.detekt.rules) { exclude(group = "org.jetbrains.kotlinx") }
    runtimeOnly(libs.detekt.formatting) { exclude(group = "org.jetbrains.kotlinx") }

    testImplementation(libs.detekt.testUtils)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly(libs.junit.platform)
    testRuntimeOnly(libs.junit4)

    intellijPlatform {
        intellijIdeaCommunity("2024.2")

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.intellij.intelliLang")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("org.jetbrains.idea.maven")

        testFramework(TestFrameworkType.Platform)

        pluginVerifier()
    }
}

kotlin {
    jvmToolchain(21)
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
    val channelName = project.version.toString()
        .split('-')
        .getOrElse(1) { "default" }
        .split('.')
        .first()
    channels.set(listOf(channelName))
}

intellijPlatform {
    pluginConfiguration {
        name.set("Detekt IntelliJ Plugin")

        ideaVersion {
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

githubRelease {
    token((project.findProperty("github.token") as? String).orEmpty())
    owner.set("detekt")
    repo.set("detekt-intellij-plugin")
    targetCommitish.set("main")
    overwrite.set(true)
    dryRun.set(false)
    body.set(
        provider {
            var changelog = project.file("changelog.md").readText()
            val sectionStart = "#### ${project.version}"
            changelog = changelog.substring(changelog.indexOf(sectionStart) + sectionStart.length)
            changelog = changelog.substring(0, changelog.indexOf("#### 1"))
            changelog.trim()
        }
    )
    val distribution = project.layout.buildDirectory
        .file("distributions/Detekt IntelliJ Plugin-${project.version}.zip")
    releaseAssets.setFrom(distribution)
}

tasks.githubRelease.configure {
    dependsOn(tasks.buildPlugin)
}
