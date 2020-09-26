import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date

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
    id("org.jetbrains.intellij").version("0.4.21")
    id("com.github.ben-manes.versions") version "0.33.0"
    kotlin("jvm").version("1.4.10")
    id("org.sonarqube") version "3.0"
    id("com.github.breadmoirai.github-release") version "2.2.12"
    id("com.jfrog.bintray") version "1.8.5"
    `maven-publish`
}

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-api:$detektVersion")
    implementation("io.gitlab.arturbosch.detekt:detekt-tooling:$detektVersion")
    runtimeOnly("io.gitlab.arturbosch.detekt:detekt-core:$detektVersion")
    runtimeOnly("io.gitlab.arturbosch.detekt:detekt-rules:$detektVersion")
    runtimeOnly("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test-utils:$detektVersion") {
        exclude(group = "org.assertj")
        exclude(group = "org.spekframework.spek2")
    }
    testImplementation("org.assertj:assertj-core:3.17.2")
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
}

configure<IntelliJPluginExtension> {
    pluginName = "Detekt IntelliJ Plugin"
    version = "2019.3"
    updateSinceUntilBuild = false
    setPlugins("IntelliLang", "Kotlin")
}

tasks.withType<PublishTask> {
    setToken(System.getenv("DETEKT_INTELLIJ_PLUGINS_TOKEN"))
}

val release by tasks.registering {
    dependsOn(
        tasks.named("githubRelease"),
        tasks.named("publishPlugin"),
        tasks.named("bintrayUpload")
    )
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

val bintrayUser: String? = findProperty("bintrayUser")?.toString()
    ?: System.getenv("BINTRAY_USER")
val bintrayKey: String? = findProperty("bintrayKey")?.toString()
    ?: System.getenv("BINTRAY_API_KEY")
val detektPublication = "DetektPublication"

bintray {
    user = bintrayUser
    key = bintrayKey
    val mavenCentralUser = System.getenv("MAVEN_CENTRAL_USER") ?: ""
    val mavenCentralPassword = System.getenv("MAVEN_CENTRAL_PW") ?: ""

    setPublications(detektPublication)

    pkg(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.PackageConfig> {
        repo = "code-analysis"
        name = "detekt"
        userOrg = "arturbosch"
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/detekt/detekt-intellij-plugin"

        version(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.VersionConfig> {
            name = project.version as? String
            released = Date().toString()

            gpg(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.GpgConfig> {
                sign = true
            })

            mavenCentralSync(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.MavenCentralSyncConfig> {
                sync = true
                user = mavenCentralUser
                password = mavenCentralPassword
                close = "1"
            })
        })
    })
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

publishing {
    publications.register<MavenPublication>(detektPublication) {
        from(components["java"])
        artifact(sourcesJar)
        artifact(javadocJar)
        artifact(project.buildDir.resolve("distributions/Detekt IntelliJ Plugin-$version.zip"))
        groupId = project.group as? String
        artifactId = project.name
        version = project.version as? String
        pom {
            description.set("Static code analysis for Kotlin")
            name.set("detekt-intellij-plugin")
            url.set("https://arturbosch.github.io/detekt")
            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("Artur Bosch")
                    name.set("Artur Bosch")
                    email.set("arturbosch@gmx.de")
                }
            }
            scm {
                url.set("https://github.com/detekt/detekt-intellij-plugin")
            }
        }
    }
}
