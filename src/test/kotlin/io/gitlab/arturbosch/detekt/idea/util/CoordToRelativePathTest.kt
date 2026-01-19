package io.gitlab.arturbosch.detekt.idea.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CoordToRelativePathTest {

    @Test
    fun `generates correct relative path from coordinate`() {
        val path = coordToRelativePath("com.example:my-plugin:1.2.3", "my-plugin-1.2.3.jar")
        assertThat(path).isEqualTo("com/example/my-plugin/1.2.3/my-plugin-1.2.3.jar")
    }

    @Test
    fun `coordToRelativePath handles invalid coordinate with less than 3 parts`() {
        val path = coordToRelativePath("invalid:coord", "some.jar")
        assertThat(path).isEqualTo("unknown/some.jar")
    }
}
