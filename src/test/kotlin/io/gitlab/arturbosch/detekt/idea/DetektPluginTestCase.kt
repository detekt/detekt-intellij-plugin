package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.detekt.test.utils.resourceAsPath
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

/**
 * Base class for 'light' test cases.
 * Beware that all test methods share the same 'project' instance.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class DetektPluginTestCase : BasePlatformTestCase() {

    init {
        // necessary in tests up from 2021.3
        // https://plugins.jetbrains.com/docs/intellij/api-changes-list-2021.html#20213
        System.setProperty("idea.force.use.core.classloader", "true")
    }

    override fun getTestDataPath(): String = resourceAsPath("testData").toString()

    override fun isWriteActionRequired(): Boolean = true

    @BeforeEach
    open fun clearConfig() {
        val config = project.service<DetektPluginSettings>()
        config.loadState(DetektPluginSettings.State())
    }

    @BeforeAll
    override fun setUp() {
        super.setUp()
    }

    @AfterAll
    override fun tearDown() {
        super.tearDown()
    }
}
