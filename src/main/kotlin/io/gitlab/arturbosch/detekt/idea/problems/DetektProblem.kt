package io.gitlab.arturbosch.detekt.idea.problems

import com.intellij.analysis.problemsView.FileProblem
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.openapi.vfs.VirtualFile
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SeverityLevel
import javax.swing.Icon

class DetektProblem(
    override val provider: DetektProblemsProvider,
    override val file: VirtualFile,
    private val finding: Finding,
) : FileProblem {

    override val text: String
        get() = finding.messageOrDescription()

    override val description: String
        get() = finding.issue.description

    override val group: String
        get() = finding.id

    override val icon: Icon
        get() = when (finding.severity) {
            SeverityLevel.ERROR -> HighlightDisplayLevel.ERROR.icon
            SeverityLevel.WARNING -> HighlightDisplayLevel.WARNING.icon
            SeverityLevel.INFO -> HighlightDisplayLevel.WEAK_WARNING.icon
        }

    override val line: Int
        get() = finding.entity.location.source.line - 1

    override val column: Int
        get() = finding.entity.location.source.column - 1
}
