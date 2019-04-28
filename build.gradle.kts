import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.intellij.IntelliJPluginExtension

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
	id("org.jetbrains.intellij").version("0.3.4")
	id("com.github.ben-manes.versions") version "0.20.0"
	kotlin("jvm").version("1.3.21")
	id("org.sonarqube") version "2.6.2"
}

dependencies {
	compile("io.gitlab.arturbosch.detekt:detekt-cli:$detektVersion")
	compile("io.gitlab.arturbosch.detekt:detekt-rules:$detektVersion")
	compile("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
}

configure<IntelliJPluginExtension> {
	pluginName = "Detekt IntelliJ Plugin"
	version = "2017.3.5"
	updateSinceUntilBuild = false
	setPlugins("IntelliLang", "Kotlin")
}
