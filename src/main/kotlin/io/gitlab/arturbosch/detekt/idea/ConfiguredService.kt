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
import io.gitlab.arturbosch.detekt.idea.util.absolutePath
import io.gitlab.arturbosch.detekt.idea.util.extractPaths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ConfiguredService(private val project: Project) {

    private val storage = DetektConfigStorage.instance(project)

    fun validate(): List<String> {
        val messages = mutableListOf<String>()

        pluginPaths()
            .filter { Files.notExists(it) }
            .forEach { messages += "Plugin jar <b>$it</b> does not exist." }

        configPaths()
            .filter { Files.notExists(it) }
            .forEach { messages += "Configuration file <b>$it</b> does not exist." }

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
            activateExperimentalRules = storage.enableAllRules
            maxIssuePolicy = RulesSpec.MaxIssuePolicy.AllowAny
        }
        config {
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

    private fun configPaths(): List<Path> = extractPaths(storage.rulesPath, project)

    private fun pluginPaths(): List<Path> = extractPaths(storage.pluginPaths, project)

    private fun baseline(): Path? = storage.baselinePath.trim()
        .takeIf { it.isNotEmpty() }
        ?.let { Paths.get(absolutePath(project, storage.baselinePath)) }

    fun execute(file: PsiFile, autoCorrect: Boolean): List<Finding> {
        val pathToAnalyze = file.virtualFile
            ?.canonicalPath
            ?: return emptyList()
        return execute(file.text, pathToAnalyze, autoCorrect)
    }

    @UseExperimental(UnstableApi::class)
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
