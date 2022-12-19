package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import org.apache.http.client.utils.URIBuilder
import java.awt.Component

// Turn off error reporting for now due to https://github.com/detekt/detekt-intellij-plugin/issues/271
class GitHubErrorReporting : ErrorReportSubmitter() {

    override fun getReportActionText(): String = "Open GitHub Issue"

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        val uri = URIBuilder("https://github.com/detekt/detekt-intellij-plugin/issues/new")
            .addParameter("title", events.mapNotNull { it.throwableText.firstLine() }.joinToString("; "))
            .addParameter("labels", "bug")
            .addParameter("body", "Please copy paste the generated bug report.")
            .build()

        val bugReport = buildString {
            appendLine("## Bug description")
            appendLine()
            appendLine(additionalInfo ?: "Please include steps to reproduce expected and actual behavior.")
            appendLine()
            appendLine("## Environment")
            appendLine()
            appendLine("- detekt Idea Version: ${PluginUtils.pluginVersion()}")
            appendLine("- Platform Version: ${PluginUtils.platformVersion()}")
            appendLine("- Platform Vendor: ${SystemInfo.JAVA_VENDOR}")
            appendLine("- Java Version: ${SystemInfo.JAVA_VERSION}")
            appendLine("- OS Name: ${SystemInfo.OS_NAME}")
            appendLine()
            appendLine("## Stacktrace")
            appendLine()
            appendLine(
                events.map { it.throwableText.trim() }
                    .joinToString(System.lineSeparator()) {
                        """
                        |```
                        |$it
                        |```
                        """.trimMargin()
                    }
            )
        }

        return runCatching {
            BrowserUtil.browse(uri)
            invokeLater { openStacktraceScratchFile(parentComponent, bugReport) }
            consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
            true
        }.getOrElse {
            consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.FAILED))
            false
        }
    }

    private fun openStacktraceScratchFile(parentComponent: Component, bugReportText: String) {
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        requireNotNull(project)
        val scratchFile = ScratchRootType.getInstance().createScratchFile(
            project,
            "detekt-idea-stacktrace.md",
            PlainTextLanguage.INSTANCE,
            bugReportText
        )
        requireNotNull(scratchFile)
        OpenFileDescriptor(project, scratchFile).navigate(true)
    }

    private fun String.firstLine(): String? = trim().split(System.lineSeparator()).firstOrNull()
}
