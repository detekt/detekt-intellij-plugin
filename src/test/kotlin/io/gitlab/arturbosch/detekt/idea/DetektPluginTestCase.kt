package io.gitlab.arturbosch.detekt.idea

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.detekt.test.utils.resourceAsPath
import io.gitlab.arturbosch.detekt.idea.config.DetektConfigStorage
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

    override fun getTestDataPath(): String = resourceAsPath("testData").toString()

    override fun isWriteActionRequired(): Boolean = true

    @BeforeEach
    open fun clearConfig() {
        val config = DetektConfigStorage.instance(project)
        config.loadState(DetektConfigStorage())
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
