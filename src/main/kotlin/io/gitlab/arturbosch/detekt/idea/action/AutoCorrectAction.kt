package io.gitlab.arturbosch.detekt.idea.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import io.gitlab.arturbosch.detekt.idea.ConfiguredService
import io.gitlab.arturbosch.detekt.idea.KOTLIN_FILE_EXTENSIONS
import io.gitlab.arturbosch.detekt.idea.util.PluginUtils
import io.gitlab.arturbosch.detekt.idea.util.isFormattingEnabled
import io.gitlab.arturbosch.detekt.idea.util.showNotification

class AutoCorrectAction : AnAction(PluginUtils.pluginIcon()) {

    private val logger = logger<AutoCorrectAction>()

    override fun update(event: AnActionEvent) {
        val file: VirtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return

        if (!file.fileSystem.isReadOnly && file.extension in KOTLIN_FILE_EXTENSIONS) {
            // enable autocorrect option only when plugin is enabled
            event.presentation.isEnabledAndVisible = project.isFormattingEnabled()
        } else {
            // hide action for non-Kotlin source files
            event.presentation.isEnabledAndVisible = false
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return
        if (virtualFile.fileSystem.isReadOnly) {
            logger.trace("Skipping readOnly file: ${virtualFile.path}")
            return
        }
        runCatching { runAction(project, psiFile) }
            .onFailure { logger.error("Unexpected error while performing auto correct action", it) }
            .getOrThrow()
    }

    internal fun runAction(project: Project, psiFile: PsiFile) {
        val service = ConfiguredService(project)
        val problems = service.validate()
        if (problems.isEmpty()) {
            val virtualFile: VirtualFile? = psiFile.virtualFile
            virtualFile?.let { forceUpdateFile(project, it) }
            service.execute(psiFile, autoCorrect = true)
            virtualFile?.refresh(false, false)
        } else {
            showNotification(problems, project)
        }
    }

    private fun forceUpdateFile(project: Project, virtualFile: VirtualFile) {
        WriteCommandAction.runWriteCommandAction(project) {
            val documentManager = FileDocumentManager.getInstance()
            val document = documentManager.getDocument(virtualFile) ?: return@runWriteCommandAction
            if (documentManager.isDocumentUnsaved(document)) {
                documentManager.saveDocument(document)
            }
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}
