package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class MockProjectTestCase {

    init {
        // necessary in tests up from 2021.3
        // https://plugins.jetbrains.com/docs/intellij/api-changes-list-2021.html#20213
        System.setProperty("idea.force.use.core.classloader", "true")
    }

    protected lateinit var project: Project

    @BeforeEach
    open fun clearConfig() {
        val config = project.service<DetektPluginSettings>()
        config.loadState(DetektPluginSettings.State())
    }

    @BeforeAll
    open fun setUp() {
        project = mockProject()
    }
}
