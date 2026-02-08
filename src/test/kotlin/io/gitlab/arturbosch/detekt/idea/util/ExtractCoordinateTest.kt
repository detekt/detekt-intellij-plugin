package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.testFramework.LightVirtualFile
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties
import org.junit.jupiter.api.Test

class ExtractCoordinateTest {

    @Test
    fun `extracts coordinate from main artifact`() {
        val props = RepositoryLibraryProperties("com.example", "my-plugin", "1.2.3")
        val vf = LightVirtualFile("my-plugin-1.2.3.jar")

        val result = extractCoordinate(vf, props, isMain = true)

        assertThat(result).isEqualTo("com.example:my-plugin:1.2.3")
    }

    @Test
    fun `extracts coordinate from transitive artifact path`() {
        val props = RepositoryLibraryProperties("main", "main", "1")
        // Mock a path that looks like a maven repo path
        val vf =
            object : LightVirtualFile("transitive-2.0.0.jar") {
                override fun getPath() = "/home/user/.m2/repository/org/some/lib/transitive/2.0.0/transitive-2.0.0.jar"
            }

        val result = extractCoordinate(vf, props, isMain = false)

        assertThat(result).isEqualTo("org.some.lib:transitive:2.0.0")
    }

    @Test
    fun `extractCoordinate returns fallback for path without repository marker`() {
        val props = RepositoryLibraryProperties("main", "main", "1")
        val vf =
            object : LightVirtualFile("artifact.jar") {
                override fun getPath() = "/some/random/path/artifact.jar"
            }

        val result = extractCoordinate(vf, props, isMain = false)

        assertThat(result).isEqualTo("unknown:artifact:unknown")
    }
}
