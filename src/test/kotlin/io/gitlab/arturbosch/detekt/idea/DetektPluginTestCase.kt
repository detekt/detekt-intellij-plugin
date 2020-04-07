package io.gitlab.arturbosch.detekt.idea

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.gitlab.arturbosch.detekt.idea.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.test.resource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.nio.file.Paths

/**
 * Base class for 'light' test cases.
 * Beware that all test methods share the same 'project' instance.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class DetektPluginTestCase : BasePlatformTestCase() {

    override fun getTestDataPath(): String = Paths.get(resource("testData")).toString()

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
