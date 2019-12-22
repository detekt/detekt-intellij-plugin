package io.gitlab.arturbosch.detekt

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.TextLocation
import io.gitlab.arturbosch.detekt.cli.FilteredDetectionResult
import io.gitlab.arturbosch.detekt.cli.baseline.BaselineFacade
import io.gitlab.arturbosch.detekt.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.config.plugins
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.util.DetektPluginService
import io.gitlab.arturbosch.detekt.util.absolutePath
import io.gitlab.arturbosch.detekt.util.ensureFileExists
import java.io.File

class DetektAnnotator : ExternalAnnotator<PsiFile, List<Finding>>() {

    private val service = DetektPluginService()

    override fun collectInformation(file: PsiFile): PsiFile = file

    override fun doAnnotate(collectedInfo: PsiFile): List<Finding> {
        val configuration = DetektConfigStorage.instance(collectedInfo.project)
        if (configuration.enableDetekt) {
            return runDetekt(collectedInfo, configuration)
        }
        return emptyList()
    }

    private fun runDetekt(
        collectedInfo: PsiFile,
        configuration: DetektConfigStorage
    ): List<Finding> {
        val virtualFile = collectedInfo.originalFile.virtualFile
        val settings = processingSettings(collectedInfo.project, virtualFile, configuration)
        if (settings != null) {
            var result = service
                .createFacade(settings, !configuration.enableFormatting)
                .run()

            result = if (configuration.baselinePath.isNotBlank()) {
                FilteredDetectionResult(
                    result,
                    BaselineFacade(File(absolutePath(collectedInfo.project, configuration.baselinePath)).toPath())
                )
            } else {
                result
            }

            return result.findings.flatMap { it.value }
        }
        return emptyList()
    }

    override fun apply(
        file: PsiFile,
        annotationResult: List<Finding>,
        holder: AnnotationHolder
    ) {
        val configuration = DetektConfigStorage.instance(file.project)
        annotationResult.forEach {
            val textRange = it.charPosition.toTextRange()
            val message = it.id + ": " + it.messageOrDescription()
            if (configuration.treatAsError) {
                holder.createErrorAnnotation(textRange, message)
            } else {
                holder.createWarningAnnotation(textRange, message)
            }
        }
    }

    private fun TextLocation.toTextRange(): TextRange = TextRange.create(start, end)

    @Suppress("ReturnCount")
    private fun processingSettings(
        project: Project,
        virtualFile: VirtualFile,
        configStorage: DetektConfigStorage
    ): ProcessingSettings? {
        val rulesPath = absolutePath(project, configStorage.rulesPath)
        val baselinePath = absolutePath(project, configStorage.baselinePath)

        if (baselinePath.isNotEmpty()) {
            if (!ensureFileExists(
                    baselinePath,
                    project,
                    "Baseline file not found",
                    "The provided detekt baseline file <b>$baselinePath</b> does not exist. Skipping detekt run."
                )
            ) {
                return null
            }
        }

        if (rulesPath.isNotEmpty()) {
            if (!ensureFileExists(
                    rulesPath,
                    project,
                    "Configuration file not found",
                    "The provided detekt configuration file <b>$rulesPath</b> does not exist. Skipping detekt run."
                )
            ) {
                return null
            }
        }

        val pluginPaths = configStorage.plugins(project) ?: return null

        return service.getProcessSettings(
            virtualFile = virtualFile,
            rulesPath = rulesPath,
            configStorage = configStorage,
            autoCorrect = false,
            pluginPaths = pluginPaths
        )
    }
}
