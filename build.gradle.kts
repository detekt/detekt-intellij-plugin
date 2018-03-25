import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.RunIdeaTask

val kotlinVersion: String by extra
val detektVersion: String  by extra
val junitPlatformVersion: String  by extra
val usedDetektGradleVersion: String by extra

project.group = "io.gitlab.arturbosch.detekt"
project.version = detektVersion

repositories {
	jcenter()
	maven { setUrl("http://dl.bintray.com/jetbrains/intellij-plugin-service") }
}

plugins {
	id("org.jetbrains.intellij").version("0.2.18")
	kotlin("jvm").version("1.2.30")
}

dependencies {
	compile("io.gitlab.arturbosch.detekt:detekt-core:1.0.0.RC6-4")
	compile("io.gitlab.arturbosch.detekt:detekt-rules:1.0.0.RC6-4")
}

configure<IntelliJPluginExtension> {
	pluginName = "Detekt IntelliJ Plugin"
	version = "2017.3.5"
	updateSinceUntilBuild = false
	setPlugins("IntelliLang", "Kotlin")
}

tasks.withType<RunIdeaTask> {
	systemProperty(
			"idea.ProcessCanceledException",
			"disabled"
	)
}
