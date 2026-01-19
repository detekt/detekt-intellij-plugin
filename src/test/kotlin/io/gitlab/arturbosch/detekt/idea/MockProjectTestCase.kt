package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class MockProjectTestCase {

    init {
        // necessary in tests up from 2021.3
        // https://plugins.jetbrains.com/docs/intellij/api-changes-list-2021.html#20213
        System.setProperty("idea.force.use.core.classloader", "true")
    }

    protected lateinit var project: Project

    @TempDir lateinit var tempDir: Path

    @BeforeEach
    open fun setupProjectAndConfig() {
        project = mockProject(tempDir.toString())
        val config = project.service<DetektPluginSettings>()
        config.loadState(DetektPluginSettings.State())
    }
}
