package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiFile
import io.github.detekt.tooling.api.DetektProvider
import io.github.detekt.tooling.api.UnexpectedError
import io.github.detekt.tooling.api.spec.ProcessingSpec
import io.github.detekt.tooling.api.spec.RulesSpec
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.UnstableApi
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import io.gitlab.arturbosch.detekt.idea.util.DirectExecutor
import io.gitlab.arturbosch.detekt.idea.util.PluginDependencyService
import io.gitlab.arturbosch.detekt.idea.util.PluginUtils
import io.gitlab.arturbosch.detekt.idea.util.SimpleAppendable
import io.gitlab.arturbosch.detekt.idea.util.absoluteBaselinePath
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * Service responsible for configuring and executing detekt analysis. It handles path validation, setting up
 * [ProcessingSpec], and running detekt either on a [PsiFile] or direct file content.
 */
class ConfiguredService(private val project: Project) {

    private val logger = logger<ConfiguredService>()
    private val settings = project.service<DetektPluginSettings>()

    private val projectBasePath = project.basePath?.let(::Path)

    /**
     * Validates the current detekt configuration. Checks if plugin JARs and configuration files exist.
     *
     * @return A list of error messages, or an empty list if validation passes.
     */
    fun validate(): List<String> {
        val messages = mutableListOf<String>()

        fun checkPaths(paths: List<Path>, messagePrefix: String) {
            paths.filter { Files.notExists(it) }.forEach { messages += "$messagePrefix <b>$it</b> does not exist." }
            paths.filter { Files.isDirectory(it) }.forEach { messages += "$messagePrefix <b>$it</b> is a directory." }
        }

        checkPaths(pluginPaths(), "Plugin jar")
        checkPaths(configPaths(), "Configuration file")

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
            activateAllRules = settings.enableAllRules
            maxIssuePolicy = RulesSpec.MaxIssuePolicy.AllowAny
        }
        config {
            // Do not throw an error during annotation mode as it is a common scenario
            // that the IntelliJ plugin is behind detekt core version-wise (new unknown config properties).
            shouldValidateBeforeAnalysis = false
            useDefaultConfig = settings.buildUponDefaultConfig
            configPaths = configPaths()
        }
        baseline {
            path = baseline()
        }
        extensions {
            fromPaths { pluginPaths() }
            if (!settings.enableFormatting) {
                disableExtension(FORMATTING_RULE_SET_ID)
            }
        }
        execution {
            executorService = DirectExecutor()
        }
        if (settings.redirectChannels) {
            logging {
                debug = settings.debug
                outputChannel = SimpleAppendable { logger.info(it) }
                errorChannel = SimpleAppendable { logger.warn(it) } // print to warn as error will trigger report dialog
            }
        }
    }

    private fun configPaths(): List<Path> =
        settings.configurationFilePaths
            .asSequence()
            .filter { it.isNotBlank() }
            .asAbsolutePaths()
            .toList()

    /**
     * Returns a list of paths to all detekt plugin JARs, including those configured manually and those downloaded via
     * Maven.
     */
    internal fun pluginPaths(): List<Path> {
        val configured = settings.pluginJarPaths
            .asSequence()
            .filter { it.isNotBlank() }
            .asAbsolutePaths()
            .toList()

        val downloadedFolder = project.service<PluginDependencyService>().getPluginFolder()
        val downloaded =
            if (downloadedFolder.exists()) {
                Files.walk(downloadedFolder).use { stream -> stream.filter { it.toString().endsWith(".jar") }.toList() }
            } else {
                emptyList()
            }

        return configured + downloaded
    }

    private fun Sequence<String>.asAbsolutePaths() =
        map { it.replace('/', File.separatorChar) }
            .map { projectBasePath?.resolve(it) ?: Paths.get(it) }

    private fun baseline(): Path? = absoluteBaselinePath(project, settings)

    /**
     * Executes detekt analysis on the provided [PsiFile].
     *
     * @param file The file to analyze.
     * @param autoCorrect Whether detekt should attempt to automatically fix issues.
     * @return A list of [Finding]s found by detekt.
     */
    fun execute(file: PsiFile, autoCorrect: Boolean): List<Finding> {
        val pathToAnalyze = file.virtualFile
            ?.canonicalPath
            ?: return emptyList()
        val content = runCatching { runReadAction { file.text } }
            .onFailure {
                logErrorIfAllowed(it, "Unexpected error while reading file content: ${file.virtualFile.path}")
            }
            .getOrThrow()
        return runCatching { execute(content, pathToAnalyze, autoCorrect) }
            .onFailure { logErrorIfAllowed(it, "Unexpected error while running detekt analysis") }
            .getOrDefault(emptyList())
    }

    private fun logErrorIfAllowed(error: Throwable, message: String) {
        if (error is ProcessCanceledException) {
            return // process cancellation is not allowed to be logged and will throw
        }
        logger.error(message, error)
    }

    /**
     * Executes detekt analysis on the provided file content.
     *
     * @param fileContent The content of the file to analyze.
     * @param filename The name of the file (used for rules matching).
     * @param autoCorrect Whether detekt should attempt to automatically fix issues.
     * @return A list of [Finding]s reported by detekt.
     */
    @OptIn(UnstableApi::class)
    fun execute(fileContent: String, filename: String, autoCorrect: Boolean): List<Finding> {
        if (filename in SPECIAL_FILES_TO_IGNORE) {
            return emptyList()
        }

        val spec: ProcessingSpec = settings(filename, autoCorrect)
        val detekt = DetektProvider.load(PluginUtils::class.java.classLoader).get(spec)
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
