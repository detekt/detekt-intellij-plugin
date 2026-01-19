package io.gitlab.arturbosch.detekt.idea.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProvidedDependenciesTest {

    @Test
    fun `should exclude detekt dependencies`() {
        assertThat(ProvidedDependencies.isProvided("io.gitlab.arturbosch.detekt", "detekt-api")).isTrue()
        assertThat(ProvidedDependencies.isProvided("io.gitlab.arturbosch.detekt", "detekt-core")).isTrue()
    }

    @Test
    fun `should exclude kotlin dependencies`() {
        assertThat(ProvidedDependencies.isProvided("org.jetbrains.kotlin", "kotlin-stdlib")).isTrue()
        assertThat(ProvidedDependencies.isProvided("org.jetbrains.kotlin", "kotlin-reflect")).isTrue()
    }

    @Test
    fun `should handle kotlinx dependencies granularly`() {
        // Excluded
        assertThat(ProvidedDependencies.isProvided("org.jetbrains.kotlinx", "kotlinx-coroutines-core")).isTrue()
        assertThat(ProvidedDependencies.isProvided("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")).isTrue()

        // Not excluded
        assertThat(ProvidedDependencies.isProvided("org.jetbrains.kotlinx", "kotlinx-serialization-json")).isFalse()
        assertThat(ProvidedDependencies.isProvided("org.jetbrains.kotlinx", "kotlinx-html-jvm")).isFalse()
    }

    @Test
    fun `should not exclude other dependencies`() {
        assertThat(ProvidedDependencies.isProvided("com.example", "my-lib")).isFalse()
        assertThat(ProvidedDependencies.isProvided("org.junit.jupiter", "junit-jupiter-api")).isFalse()
    }

    @Test
    fun `should exclude dev detekt dependencies`() {
        assertThat(ProvidedDependencies.isProvided("dev.detekt", "detekt-api")).isTrue()
        assertThat(ProvidedDependencies.isProvided("dev.detekt", "detekt-core")).isTrue()
        assertThat(ProvidedDependencies.isProvided("dev.detekt", "detekt-psi-utils")).isTrue()
    }

    @Test
    fun `getExclusions returns all provided group patterns`() {
        val exclusions = ProvidedDependencies.getExclusions()

        assertThat(exclusions).contains("io.gitlab.arturbosch.detekt:*")
        assertThat(exclusions).contains("dev.detekt:*")
        assertThat(exclusions).contains("org.jetbrains.kotlin:*")
        assertThat(exclusions).contains("org.jetbrains.kotlinx:kotlinx-coroutines-*")
    }
}
