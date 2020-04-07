package io.gitlab.arturbosch.detekt.idea.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.gitlab.arturbosch.detekt.idea.ConfiguredService
import io.gitlab.arturbosch.detekt.idea.KOTLIN_FILE_EXTENSIONS
import io.gitlab.arturbosch.detekt.idea.util.isDetektEnabled
import io.gitlab.arturbosch.detekt.idea.util.showNotification
import java.nio.file.Paths

class AutoCorrectAction : AnAction() {

    override fun update(event: AnActionEvent) {
        val file: VirtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return

        if (file.extension in KOTLIN_FILE_EXTENSIONS) {
            // enable auto correct option only when plugin is enabled
            event.presentation.isEnabledAndVisible = project.isDetektEnabled()
        } else {
            // hide action for non-Kotlin source files
            event.presentation.isEnabledAndVisible = false
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val project = event.getData(CommonDataKeys.PROJECT)
        println("Update file") // TODO

        if (virtualFile != null && project != null) {
            val service = ConfiguredService(project)
            val problems = service.validate()
            if (problems.isEmpty()) {
                forceUpdateFile(project, virtualFile)
                val pathToAnalyze = Paths.get(virtualFile.path)
                service.execute(pathToAnalyze, autoCorrect = true)
                virtualFile.refresh(false, false)
                println("AutoCorrect should be complete") // TODO
            } else {
                showNotification(problems, project)
            }
        }
    }

    private fun forceUpdateFile(project: Project, virtualFile: VirtualFile) {
        WriteCommandAction.runWriteCommandAction(project) {
            val documentManager = FileDocumentManager.getInstance()
            val document = documentManager.getDocument(virtualFile)
            if (document != null) {
                documentManager.saveDocument(document)
                println("Force update was completed") // TODO
            }
        }
    }
}
