package io.gitlab.arturbosch.detekt.idea

import io.github.detekt.psi.FilePath
import io.github.detekt.tooling.api.AnalysisResult
import io.github.detekt.tooling.api.Detekt
import io.github.detekt.tooling.api.DetektProvider
import io.github.detekt.tooling.api.spec.ProcessingSpec
import io.github.detekt.tooling.internal.DefaultAnalysisResult
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Detektion
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Location
import io.gitlab.arturbosch.detekt.api.Notification
import io.gitlab.arturbosch.detekt.api.ProjectMetric
import io.gitlab.arturbosch.detekt.api.RuleSetId
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.api.TextLocation
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import java.nio.file.Path
import java.nio.file.Paths

class DetektProviderStub : DetektProvider {

    override val priority: Int = 1

    override fun get(processingSpec: ProcessingSpec): Detekt = DetektStub()
}

class DetektStub : Detekt {

    override fun run(): AnalysisResult {
        throw UnsupportedOperationException()
    }

    override fun run(path: Path): AnalysisResult {
        throw UnsupportedOperationException()
    }

    override fun run(sourceCode: String, filename: String): AnalysisResult {
        if (!filename.contains("Poko.kt")) {
            throw UnsupportedOperationException("Only Poko.kt runs are supported.")
        }
        return DefaultAnalysisResult(object : Detektion {
            override val findings: Map<RuleSetId, List<Finding>> = mapOf(
                "empty-blocks" to listOf(
                    CodeSmell(
                        Issue(
                            "EmptyDefaultConstructor",
                            Severity.Minor,
                            "empty",
                            Debt.FIVE_MINS
                        ),
                        Entity(
                            "Poko",
                            "testData.Poko.kt",
                            Location(
                                SourceLocation(3, 10),
                                TextLocation(28, 30),
                                FilePath(Paths.get(filename))
                            )
                        ),
                        "empty constructor"
                    )
                )
            )
            override val metrics: Collection<ProjectMetric> = emptyList()
            override val notifications: Collection<Notification> = emptyList()

            override fun add(notification: Notification) {
                // ignore
            }

            override fun add(projectMetric: ProjectMetric) {
                // ignore
            }

            override fun <V> addData(key: Key<V>, value: V) {
                // ignore
            }

            override fun <V> getData(key: Key<V>): V? {
                // ignore
                return null
            }
        })
    }

    override fun run(files: Collection<KtFile>, bindingContext: BindingContext): AnalysisResult {
        throw UnsupportedOperationException()
    }
}
