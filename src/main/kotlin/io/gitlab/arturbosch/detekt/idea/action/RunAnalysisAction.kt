package io.gitlab.arturbosch.detekt.idea.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import io.gitlab.arturbosch.detekt.idea.ConfiguredService
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.KOTLIN_FILE_EXTENSIONS
import io.gitlab.arturbosch.detekt.idea.problems.FindingsManager
import io.gitlab.arturbosch.detekt.idea.util.PluginUtils
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.idea.KotlinLanguage

class RunAnalysisAction : AnAction(PluginUtils.pluginIcon()) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val isDirectoryOrKotlinFile = selectedFiles.any { it.isDirectory || it.extension in KOTLIN_FILE_EXTENSIONS }
        event.presentation.isEnabledAndVisible = isDirectoryOrKotlinFile
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY)

        if (selectedFiles.isNullOrEmpty()) {
            return
        }

        val project = e.project ?: return
        val psiFiles = runReadAction {
            findAllFilesRecursively(selectedFiles)
                .mapNotNull { PsiManager.getInstance(project).findFile(it) }
                .filter { it.language.id == KotlinLanguage.INSTANCE.id }
                .toList()
        }

        val job = object : Task.Backgroundable(project, DetektBundle.message("detekt.analysis.run"), true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.checkCanceled()
                val service = ConfiguredService(project)
                val findingsManager = FindingsManager.getInstance(project)
                indicator.checkCanceled()
                for (psi in psiFiles) {
                    val findings = service.execute(psi, false)
                    indicator.checkCanceled()
                    findingsManager.put(psi.virtualFile, findings)
                }
                findingsManager.notifyListeners()
            }
        }

        ProgressManager.getInstance().run(job)
    }

    private fun findAllFilesRecursively(selectedFiles: Array<VirtualFile>): Sequence<VirtualFile> = sequence {
        val dirStack = ArrayDeque<VirtualFile>()
        dirStack.addAll(selectedFiles)
        while (dirStack.isNotEmpty()) {
            val currentVirtualFile = dirStack.pop()
            if (currentVirtualFile.isDirectory) {
                dirStack.addAll(currentVirtualFile.children)
            } else {
                yield(currentVirtualFile)
            }
        }
    }
}
