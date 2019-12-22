package io.gitlab.arturbosch.detekt.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import io.gitlab.arturbosch.detekt.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.config.plugins
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.util.DetektPluginService
import io.gitlab.arturbosch.detekt.util.KOTLIN_FILE_EXTENSION
import io.gitlab.arturbosch.detekt.util.absolutePath
import java.io.File

class AutoCorrectAction : AnAction() {

    private var service = DetektPluginService()

    override fun update(event: AnActionEvent) {
        val file: VirtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val configuration = DetektConfigStorage.instance(project)

        if (file.extension == KOTLIN_FILE_EXTENSION) {
            // enable auto correct option only when plugin is enabled
            event.presentation.isEnabledAndVisible = configuration.enableDetekt
        } else {
            // hide action for non-Kotlin source files
            event.presentation.isEnabledAndVisible = false
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val project = event.getData(CommonDataKeys.PROJECT)
        println("Update file")
        forceUpdateFile(project, virtualFile)

        if (virtualFile != null && project != null) {
            val configuration = DetektConfigStorage.instance(project)
            val settings = processingSettings(project, virtualFile, configuration)
            if (settings != null) {
                service.createFacade(settings).run()
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
                println("Force update was completed")
                return@Computable false
            }
            true
        })
    }

    @Suppress("ReturnCount")
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
        val plugins = configStorage.plugins(project) ?: return null
        return service.getProcessSettings(
            virtualFile = virtualFile,
            rulesPath = rulesPath,
            configStorage = configStorage,
            autoCorrect = true,
            pluginPaths = plugins
        )
    }
}
