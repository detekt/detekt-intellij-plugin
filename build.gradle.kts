import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion: String by extra
val detektVersion: String by extra
val junitVersion: String by extra
val striktVersion: String by extra
val detektIntellijPluginVersion: String by extra

project.group = "io.gitlab.arturbosch.detekt"
project.version = detektIntellijPluginVersion

repositories {
    jcenter()
    maven { setUrl("http://dl.bintray.com/jetbrains/intellij-plugin-service") }
}

plugins {
    id("org.jetbrains.intellij").version("0.4.18")
    id("com.github.ben-manes.versions") version "0.27.0"
    kotlin("jvm").version("1.3.71")
    id("org.sonarqube") version "2.8"
    id("com.github.breadmoirai.github-release") version "2.2.12"
}

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-api:$detektVersion")
    implementation("io.gitlab.arturbosch.detekt:detekt-core:$detektVersion")
    implementation("io.gitlab.arturbosch.detekt:detekt-cli:$detektVersion")
    implementation("io.gitlab.arturbosch.detekt:detekt-rules:$detektVersion")
    implementation("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:$detektVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configure<IntelliJPluginExtension> {
    pluginName = "Detekt IntelliJ Plugin"
    version = "2019.3"
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
