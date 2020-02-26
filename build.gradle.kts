import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion: String by extra
val detektVersion: String by extra
val detektIntellijPluginVersion: String by extra

project.group = "io.gitlab.arturbosch.detekt"
project.version = detektIntellijPluginVersion

repositories {
	jcenter()
	maven { setUrl("http://dl.bintray.com/jetbrains/intellij-plugin-service") }
}

plugins {
	id("org.jetbrains.intellij").version("0.4.15")
	id("com.github.ben-manes.versions") version "0.27.0"
	kotlin("jvm").version("1.3.61")
	id("org.sonarqube") version "2.8"
}

dependencies {
	implementation("io.gitlab.arturbosch.detekt:detekt-api:$detektVersion")
	implementation("io.gitlab.arturbosch.detekt:detekt-core:$detektVersion")
	implementation("io.gitlab.arturbosch.detekt:detekt-cli:$detektVersion")
	implementation("io.gitlab.arturbosch.detekt:detekt-rules:$detektVersion")
	implementation("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "1.8"
}

configure<IntelliJPluginExtension> {
	pluginName = "Detekt IntelliJ Plugin"
	version = "2019.3"
	updateSinceUntilBuild = false
	setPlugins("IntelliLang", "Kotlin")
}
