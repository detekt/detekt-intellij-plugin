package io.gitlab.arturbosch.detekt

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.options.newEditor.SettingsDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.TextLocation
import io.gitlab.arturbosch.detekt.cli.CliArgs
import io.gitlab.arturbosch.detekt.cli.FilteredDetectionResult
import io.gitlab.arturbosch.detekt.cli.baseline.BaselineFacade
import io.gitlab.arturbosch.detekt.cli.loadConfiguration
import io.gitlab.arturbosch.detekt.config.DetektConfig
import io.gitlab.arturbosch.detekt.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.config.NoAutoCorrectConfig
import io.gitlab.arturbosch.detekt.core.DetektFacade
import io.gitlab.arturbosch.detekt.core.FileProcessorLocator
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.core.RuleSetLocator
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
        WriteCommandAction.runWriteCommandAction(collectedInfo.project, Computable<Boolean> {
            val documentManager = FileDocumentManager.getInstance()
            val document = documentManager.getDocument(collectedInfo.virtualFile)
            if (document != null) {
                documentManager.saveDocument(document)
                return@Computable false
            }
            true
        })

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

            if (settings.autoCorrect) {
                virtualFile.refresh(false, false)
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
                    "The provided detekt baseline file <b>${baselinePath}</b> does not exist. Skipping detekt run."
                )
            )
                return null
        }

        if (rulesPath.isNotEmpty()) {
            if (!ensureFileExists(
                    rulesPath,
                    project,
                    "Configuration file not found",
                    "The provided detekt configuration file <b>${rulesPath}</b> does not exist. Skipping detekt run."
                )
            )
                return null
        }

        return ProcessingSettings(
            inputPath = Paths.get(virtualFile.path),
            autoCorrect = configStorage.autoCorrect,
            config = NoAutoCorrectConfig(CliArgs().apply {
                config = rulesPath
                failFast = configStorage.failFast
                buildUponDefaultConfig = configStorage.buildUponDefaultConfig
            }.loadConfiguration(), configStorage.autoCorrect),
            executorService = ForkJoinPool.commonPool()
        )
    }

    private fun absolutePath(project: Project, path: String): String {
        return if (path.isBlank() || File(path).isAbsolute)
            path
        else
            project.basePath + "/" + path
    }

    private fun ensureFileExists(path: String, project: Project, title: String, content: String): Boolean {
        if (!File(path).exists()) {
            val n = Notification(
                "Detekt",
                title,
                content,
                NotificationType.WARNING
            )
            n.addAction(object : AnAction("Open Detekt projects settings") {
                override fun actionPerformed(e: AnActionEvent) {
                    val dialog = SettingsDialog(project, "Detekt project settings", DetektConfig(project), true, true)
                    ApplicationManager.getApplication().invokeLater(dialog::show);
                }
            })
            n.notify(project)
            return false
        }
        return true
    }

    private fun createFacade(settings: ProcessingSettings, configuration: DetektConfigStorage): DetektFacade {
        var providers = RuleSetLocator(settings).load()
        if (!configuration.enableFormatting) {
            providers = providers.filterNot { it.ruleSetId == "formatting" }
        }
        val processors = FileProcessorLocator(settings).load()
        return DetektFacade.create(settings, providers, processors)
    }
}
