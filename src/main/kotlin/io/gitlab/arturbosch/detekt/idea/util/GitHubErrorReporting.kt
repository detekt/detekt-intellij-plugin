package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import org.apache.http.client.utils.URIBuilder
import java.awt.Component

class GitHubErrorReporting : ErrorReportSubmitter() {

    override fun getReportActionText(): String = "Open GitHub Issue"

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        val uri = URIBuilder("https://github.com/detekt/detekt-intellij-plugin/issues/new")
            .addParameter("title", events.mapNotNull { it.throwableText.firstLine() }.joinToString("; "))
            .addParameter("labels", "bug")
            .addParameter("body", formatIssueBody(additionalInfo))
            .build()

        return runCatching {
            BrowserUtil.browse(uri)
            ApplicationManager.getApplication()
                .invokeLater { openStacktraceScratchFile(parentComponent, formatStacktrace(events)) }
            consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
            true
        }.getOrElse {
            consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.FAILED))
            false
        }
    }

    private fun openStacktraceScratchFile(parentComponent: Component, stacktrace: String) {
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        requireNotNull(project)
        val scratchFile = ScratchRootType.getInstance()
            .createScratchFile(project, "detekt-idea-stacktrace.md", PlainTextLanguage.INSTANCE, stacktrace)
        requireNotNull(scratchFile)
        OpenFileDescriptor(project, scratchFile).navigate(true)
    }

    private fun formatStacktrace(events: Array<out IdeaLoggingEvent>): String =
        "Please copy following stacktrace to the opened issue body." +
                System.lineSeparator() +
                System.lineSeparator() +
                events.joinToString(System.lineSeparator()) {
                    """
```
${it.throwableText.trim()}
```
            """.trimIndent()
                }

    private fun formatIssueBody(additionalInfo: String?): String = """
        ## Bug description
        ${additionalInfo ?: "Please include steps to reproduce expected and actual behavior."}
    
        ## Environment
        - detekt Idea Version: ${PluginUtils.pluginVersion()}
        - Platform Version: ${PluginUtils.platformVersion()}
        - Platform Vendor: ${SystemInfo.JAVA_VENDOR}
        - Java Version: ${SystemInfo.JAVA_VERSION}
        - OS Name: ${SystemInfo.OS_NAME}
    
        ## Stacktrace
        
        ```
        Please include the stacktrace from the temporary scratch issue.
        ```
    """.trimIndent()

    private fun String.firstLine(): String? = trim().split(System.lineSeparator()).firstOrNull()
}
