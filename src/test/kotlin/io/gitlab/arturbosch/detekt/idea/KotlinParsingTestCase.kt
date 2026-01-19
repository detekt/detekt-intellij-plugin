package io.gitlab.arturbosch.detekt.idea

import com.intellij.mock.MockApplication
import com.intellij.mock.MockPsiFile
import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class KotlinParsingTestCase : MockProjectTestCase() {

    private lateinit var rootDisposable: Disposable

    @BeforeAll
    fun setUpProject(@TempDir tempDir: Path) {
        rootDisposable = Disposable { }
        project = heavyMockProject(rootDisposable, tempDir.toString())
    }

    override fun setupProjectAndConfig() {
        val config = project.service<DetektPluginSettings>()
        config.loadState(DetektPluginSettings.State())
    }

    @AfterAll
    fun tearDown() {
        Disposer.dispose(rootDisposable)
        rootDisposable = Disposable { }
        ApplicationManager.setApplication(
            MockApplication(rootDisposable),
            { FileTypeManager.getInstance() },
            rootDisposable,
        )
    }

    fun parseFile(testFile: Path): PsiFile {
        val virtualFile = object : MockVirtualFile(testFile.toString(), testFile.readText()) {
            // get rid of "MOCK_ROOT:/" prefix, it is not a valid windows path
            override fun getPath(): String = testFile.toString()
        }
        return MockPsiFile(virtualFile, PsiManager.getInstance(project)).apply {
            putUserData(TEST_KOTLIN_LANGUAGE_ID_KEY, KotlinLanguage.INSTANCE.id)
        }
    }
}
