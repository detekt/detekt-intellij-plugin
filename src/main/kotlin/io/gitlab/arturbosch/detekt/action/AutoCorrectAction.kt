package io.gitlab.arturbosch.detekt.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import io.gitlab.arturbosch.detekt.cli.CliArgs
import io.gitlab.arturbosch.detekt.cli.loadConfiguration
import io.gitlab.arturbosch.detekt.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.util.FileExtensions
import io.gitlab.arturbosch.detekt.util.absolutePath
import io.gitlab.arturbosch.detekt.util.createFacade
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.ForkJoinPool

class AutoCorrectAction : AnAction() {

    override fun update(event: AnActionEvent) {
        val file: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)

        println(file?.extension)
        if (file?.extension != FileExtensions.KOTLIN_FILE_EXTENSION) {
            // hide action for non-Kotlin source files
            event.presentation.isEnabledAndVisible = false
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        println("Start autocorrect")
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val project = event.getData(CommonDataKeys.PROJECT)
        println("Update file")
        forceUpdateFile(project, virtualFile)

        if (virtualFile != null && project != null) {
            val configuration = DetektConfigStorage.instance(project)
            val settings = processingSettings(project, virtualFile, configuration)

            settings?.let {
                createFacade(settings, configuration).run()

                virtualFile.refresh(false, false)
                println("AutoCorrect should be complete")
            }
        }
    }

    private fun forceUpdateFile(project: Project?, virtualFile: VirtualFile?) {
        WriteCommandAction.runWriteCommandAction(project, Computable<Boolean> {
            val documentManager = FileDocumentManager.getInstance()
            val document = documentManager.getDocument(virtualFile!!)
            if (document != null) {
                documentManager.saveDocument(document)
                return@Computable false
            }
            true
        })
    }

    private fun processingSettings(
        project: Project,
        virtualFile: VirtualFile,
        configStorage: DetektConfigStorage
    ): ProcessingSettings? {
        val rulesPath = absolutePath(project, configStorage.rulesPath)
        if (rulesPath.isNotEmpty()) {
            if (!File(rulesPath).exists()) {
                return null
            }
        }

        return ProcessingSettings(
            inputPath = Paths.get(virtualFile.path),
            autoCorrect = true,
            config = CliArgs().apply {
                config = rulesPath
                failFast = configStorage.failFast
                buildUponDefaultConfig = configStorage.buildUponDefaultConfig
            }.loadConfiguration(),
            executorService = ForkJoinPool.commonPool()
        )
    }
}
