package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
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
import io.gitlab.arturbosch.detekt.idea.util.DirectExecutor
import io.gitlab.arturbosch.detekt.idea.util.PluginUtils
import io.gitlab.arturbosch.detekt.idea.util.absolutePath
import io.gitlab.arturbosch.detekt.idea.util.extractPaths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ServiceLoader

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
            executorService = DirectExecutor()
        }
    }

    private fun configPaths(): List<Path> = extractPaths(storage.configPaths, project)

    private fun pluginPaths(): List<Path> = extractPaths(storage.pluginPaths, project)

    private fun baseline(): Path? = storage.baselinePath.trim()
        .takeIf { it.isNotEmpty() }
        ?.let { Paths.get(absolutePath(project, storage.baselinePath)) }

    fun execute(file: PsiFile, autoCorrect: Boolean): List<Finding> {
        val pathToAnalyze = file.virtualFile
            ?.canonicalPath
            ?: return emptyList()
        val content = runReadAction { file.text }
        return execute(content, pathToAnalyze, autoCorrect)
    }

    @OptIn(UnstableApi::class)
    fun execute(fileContent: String, filename: String, autoCorrect: Boolean): List<Finding> {
        if (filename == SPECIAL_FILENAME_FOR_DEBUGGING) {
            return emptyList()
        }

        val spec: ProcessingSpec = settings(filename, autoCorrect)
        val detekt = loadProviderConsiderStubbed().get(spec)
        val result = if (autoCorrect) {
            runWriteAction { detekt.run(fileContent, filename) }
        } else {
            detekt.run(fileContent, filename)
        }

        when (val error = result.error) {
            is UnexpectedError -> throw error.cause
            null -> Unit
            else -> throw error
        }

        return result.container?.findings?.flatMap { it.value } ?: emptyList()
    }
}

fun loadProviderConsiderStubbed(): DetektProvider {
    val providers = ServiceLoader.load(DetektProvider::class.java, PluginUtils::class.java.classLoader).toList()
    return providers
        .find { it.javaClass.simpleName == "StubDetektProvider" }
        ?: providers.first()
}
