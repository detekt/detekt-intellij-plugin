package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
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
open class DetektPluginTestCase {

    init {
        // necessary in tests up from 2021.3
        // https://plugins.jetbrains.com/docs/intellij/api-changes-list-2021.html#20213
        System.setProperty("idea.force.use.core.classloader", "true")
    }

    protected lateinit var myFixture: CodeInsightTestFixture

    protected val project: Project
        get() = myFixture.project

    protected val psiManager get(): PsiManager = PsiManager.getInstance(project)

    protected open fun getTestDataPath(): String = resourceAsPath("testData").toString()

    @BeforeEach
    open fun clearConfig() {
        val config = myFixture.project.service<DetektPluginSettings>()
        config.loadState(DetektPluginSettings.State())
    }

    @BeforeAll
    fun setUp() {
        Registry.get("ide.propagate.context").setValue(false)
        Registry.get("indexing.filename.over.vfs").setValue(false)
        Registry.get("ide.propagate.cancellation").setValue(false)
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(
            null as LightProjectDescriptor?,
            this::class.java.simpleName
        )
        val fixture = fixtureBuilder.fixture

        myFixture = IdeaTestFixtureFactory.getFixtureFactory()
            .createCodeInsightFixture(fixture, createTempDirTestFixture())

        myFixture.testDataPath = getTestDataPath()
        myFixture.setUp()
    }

    protected open fun createTempDirTestFixture(): TempDirTestFixture {
        val policy = IdeaTestExecutionPolicy.current()
        return if (policy != null) {
            policy.createTempDirTestFixture()
        } else {
            LightTempDirTestFixtureImpl(true)
        }
    }

    @AfterAll
    fun tearDown() {
        runCatching { myFixture.tearDown() }
    }
}
