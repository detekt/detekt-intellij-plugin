package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiFile
import com.intellij.util.io.exists
import io.github.detekt.tooling.api.DetektProvider
import io.github.detekt.tooling.api.UnexpectedError
import io.github.detekt.tooling.api.spec.ProcessingSpec
import io.github.detekt.tooling.api.spec.RulesSpec
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.UnstableApi
import io.gitlab.arturbosch.detekt.idea.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.idea.util.DirectExecuter
import io.gitlab.arturbosch.detekt.idea.util.DownloadFileImpl
import io.gitlab.arturbosch.detekt.idea.util.absolutePath
import io.gitlab.arturbosch.detekt.idea.util.extractPaths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ConfiguredService(private val project: Project) {

    private val storage = DetektConfigStorage.instance(project)
    private val download = DownloadFileImpl()
    private var configPaths: String = ""
    private var pluginPaths: String = ""
    private var baseline: String = ""

    fun validate(): List<String> {
        val messages = mutableListOf<String>()

        if (!validateHttpPlugin(storage.pluginPaths)) {
            messages += "Plugin jar <b>${storage.pluginPaths}</b> download failed."
        }
        pluginPaths()
            .filter { Files.notExists(it) }
            .forEach { messages += "Plugin jar <b>$it</b> does not exist." }

        if (!validateHttpConfig(storage.configPaths)) {
            messages += "Configuration file <b>${storage.configPaths}</b> download failed."
        }
        configPaths()
            .filter { Files.notExists(it) }
            .forEach { messages += "Configuration file <b>$it</b> does not exist." }

        if (!validateHttpBaseline(storage.baselinePath)) {
            messages += "The provided baseline file <b>${storage.baselinePath}</b> download failed."
        }
        val baseline = baseline()
        if (baseline != null && !baseline.exists()) {
            messages += "The provided baseline file <b>$baseline</b> does not exist."
        }

        return messages
    }

    private fun settings(filename: String, autoCorrect: Boolean) = ProcessingSpec {
        project {
            basePath = project.guessProjectDir()?.canonicalPath?.let { Paths.get(it) }
            inputPaths = listOf(Paths.get(filename))
        }
        rules {
            this.autoCorrect = autoCorrect
            activateAllRules = storage.enableAllRules
            maxIssuePolicy = RulesSpec.MaxIssuePolicy.AllowAny
        }
        config {
            // Do not throw an error during annotation mode as it is a common scenario
            // that the IntelliJ plugin is behind detekt core version-wise (new unknown config properties).
            shouldValidateBeforeAnalysis = false
            useDefaultConfig = storage.buildUponDefaultConfig
            configPaths = configPaths()
        }
        baseline {
            path = baseline()
        }
        extensions {
            fromPaths { pluginPaths() }
            if (!storage.enableFormatting) {
                disableExtension(FORMATTING_RULE_SET_ID)
            }
        }
        execution {
            executorService = DirectExecuter()
        }
    }

    private fun configPaths(): List<Path> = extractPaths(configPaths, project)

    private fun pluginPaths(): List<Path> = extractPaths(pluginPaths, project)

    private fun validateHttpPlugin(path: String): Boolean {
        if (!path.startsWith("http")) {
            pluginPaths = storage.pluginPaths
            return true
        }

        val file = download.download(path)
        if (file != null) {
            pluginPaths = file.path
            return true
        }
        return false
    }

    private fun validateHttpConfig(path: String): Boolean {
        if (!path.startsWith("http")) {
            configPaths = storage.configPaths
            return true
        }

        val file = download.download(path)
        if (file != null) {
            configPaths = file.path
            return true
        }
        return false
    }

    private fun validateHttpBaseline(path: String): Boolean {
        if (!path.startsWith("http")) {
            baseline = storage.baselinePath
            return true
        }

        val file = download.download(path)
        if (file != null) {
            baseline = file.path
            return true
        }
        return false
    }

    private fun baseline(): Path? = baseline.trim()
        .takeIf { it.isNotEmpty() }
        ?.let { Paths.get(absolutePath(project, baseline)) }

    fun execute(file: PsiFile, autoCorrect: Boolean): List<Finding> {
        val pathToAnalyze = file.virtualFile
            ?.canonicalPath
            ?: return emptyList()
        return execute(file.text, pathToAnalyze, autoCorrect)
    }

    @OptIn(UnstableApi::class)
    fun execute(fileContent: String, filename: String, autoCorrect: Boolean): List<Finding> {
        if (filename == SPECIAL_FILENAME_FOR_DEBUGGING) {
            return emptyList()
        }

        val spec: ProcessingSpec = settings(filename, autoCorrect)
        val detekt = DetektProvider.load().get(spec)
        val result = detekt.run(fileContent, filename)

        when (val error = result.error) {
            is UnexpectedError -> throw error.cause
            null -> Unit
            else -> throw error
        }

        return result.container?.findings?.flatMap { it.value } ?: emptyList()
    }
}
