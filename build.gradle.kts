import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date

val kotlinVersion: String by extra
val detektVersion: String by extra
val junitVersion: String by extra
val assertjVersion: String by extra
val detektIntellijPluginVersion: String by extra

project.group = "io.gitlab.arturbosch.detekt"
project.version = detektIntellijPluginVersion

repositories {
    jcenter()
    maven { setUrl("http://dl.bintray.com/jetbrains/intellij-plugin-service") }
}

plugins {
    id("org.jetbrains.intellij").version("0.7.2")
    id("com.github.ben-manes.versions") version "0.33.0"
    kotlin("jvm").version("1.4.10")
    id("org.sonarqube") version "3.0"
    id("com.github.breadmoirai.github-release") version "2.2.12"
}

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-api:$detektVersion")
    implementation("io.gitlab.arturbosch.detekt:detekt-tooling:$detektVersion")
    runtimeOnly("io.gitlab.arturbosch.detekt:detekt-core:$detektVersion")
    runtimeOnly("io.gitlab.arturbosch.detekt:detekt-rules:$detektVersion")
    runtimeOnly("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test-utils:$detektVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf(
        "-Xopt-in=kotlin.RequiresOptIn"
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.publishPlugin {
    setToken(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken"))
}

configure<IntelliJPluginExtension> {
    pluginName = "Detekt IntelliJ Plugin"
    version = "2020.3"
    updateSinceUntilBuild = false
    setPlugins("IntelliLang", "Kotlin")
}

githubRelease {
    token(project.findProperty("github.token") as? String ?: "")
    owner.set("detekt")
    repo.set("detekt-intellij-plugin")
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

val sourcesJar by tasks.creating(Jar::class) {
    dependsOn(tasks.classes)
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    from(tasks.javadoc)
    archiveClassifier.set("javadoc")
}

artifacts {
    archives(sourcesJar)
    archives(javadocJar)
}
