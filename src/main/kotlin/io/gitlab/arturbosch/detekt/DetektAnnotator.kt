package io.gitlab.arturbosch.detekt

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.TextLocation
import io.gitlab.arturbosch.detekt.cli.CliArgs
import io.gitlab.arturbosch.detekt.cli.FilteredDetectionResult
import io.gitlab.arturbosch.detekt.cli.baseline.BaselineFacade
import io.gitlab.arturbosch.detekt.cli.loadConfiguration
import io.gitlab.arturbosch.detekt.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.util.absolutePath
import io.gitlab.arturbosch.detekt.util.createFacade
import io.gitlab.arturbosch.detekt.util.ensureFileExists
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.ForkJoinPool

/**
 * @author Dmytro Primshyts
 * @author Artur Bosch
 */
class DetektAnnotator : ExternalAnnotator<PsiFile, List<Finding>>() {

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

        return settings?.let {
            val detektion = createFacade(settings, configuration).run()

            val result = if (configuration.baselinePath.isNotBlank()) {
                FilteredDetectionResult(detektion, BaselineFacade(File(absolutePath(collectedInfo.project, configuration.baselinePath)).toPath()))
            } else {
                detektion
            }

            result.findings.flatMap { it.value }
        } ?: emptyList()
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
            )
                return null
        }

        if (rulesPath.isNotEmpty()) {
            if (!ensureFileExists(
                    rulesPath,
                    project,
                    "Configuration file not found",
                    "The provided detekt configuration file <b>$rulesPath</b> does not exist. Skipping detekt run."
                )
            )
                return null
        }

        return ProcessingSettings(
            inputPath = Paths.get(virtualFile.path),
            config = CliArgs().apply {
                config = rulesPath
                failFast = configStorage.failFast
                buildUponDefaultConfig = configStorage.buildUponDefaultConfig
            }.loadConfiguration(),
            executorService = ForkJoinPool.commonPool()
        )
    }
}
