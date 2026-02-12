package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.jarRepository.JarRepositoryAuthenticationDataProvider
import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoriesConfiguration
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsContexts.ProgressText
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.jetbrains.idea.maven.aether.ArtifactDependencyNode
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager
import org.jetbrains.idea.maven.aether.ProgressConsumer
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties
import java.io.File
import java.util.EnumSet

/**
 * Resolver for Maven dependency trees.
 */
interface MavenTreeResolver {
    /**
     * Resolves the dependency tree for the given Maven coordinates.
     *
     * @param groupId the group ID of the Maven artifact
     * @param artifactId the artifact ID of the Maven artifact
     * @param version the version of the Maven artifact
     * @param project the IntelliJ project
     * @return the root node of the resolved dependency tree, or null if resolution fails
     */
    @Suppress("LongParameterList") // Not worth grouping up
    fun resolveDependenciesTree(
        groupId: String,
        artifactId: String,
        version: String,
        project: Project,
        shouldDownload: Boolean,
        exclusions: List<String>,
        indicator: ProgressIndicator?,
    ): Pair<ArtifactDependencyNode?, Map<String, File>>

    companion object {
        /**
         * The default implementation of [MavenTreeResolver].
         */
        val Default: MavenTreeResolver = DefaultMavenTreeResolver
    }
}

/**
 * Default implementation of [MavenTreeResolver] that uses IntelliJ's [JarRepositoryManager] and
 * [ArtifactRepositoryManager] to resolve dependencies.
 */
private object DefaultMavenTreeResolver : MavenTreeResolver {
    private val logger = thisLogger()

    override fun resolveDependenciesTree(
        groupId: String,
        artifactId: String,
        version: String,
        project: Project,
        shouldDownload: Boolean,
        exclusions: List<String>,
        indicator: ProgressIndicator?,
    ): Pair<ArtifactDependencyNode?, Map<String, File>> {
        val props = RepositoryLibraryProperties(groupId, artifactId, version, true, emptyList())

        logger.debug("Resolving tree for $props")
        val description = RepositoryLibraryDescription.findDescription(props)
        logger.debug("Resolved description: $description")
        val (dependenciesTree, resolvedFiles) =
            loadDependenciesTree(description, version, project, shouldDownload, exclusions, indicator)
        logger.debug("Resolved tree:\n${dependenciesTree.printableTreeString()}")
        return dependenciesTree to resolvedFiles
    }

    /**
     * Loads the dependency tree for the given [description].
     *
     * Inlined from [JarRepositoryManager.loadDependenciesTree] to remove the modal progress dialog and allow for more
     * flexible progress reporting.
     */
    @Suppress("LongParameterList") // Not worth grouping up
    private fun loadDependenciesTree(
        description: RepositoryLibraryDescription?,
        version: String,
        project: Project,
        shouldDownload: Boolean,
        exclusions: List<String>,
        indicator: ProgressIndicator?,
    ): Pair<ArtifactDependencyNode?, Map<String, File>> {
        if (description == null) return null to emptyMap()
        logger.debug("Loading dependencies tree for ${description.getMavenCoordinates(version)}")
        val repositories = RemoteRepositoriesConfiguration.getInstance(project).repositories
        logger.debug("Creating remote repositories for [${repositories.joinToString(", ") { it.url }}]")
        val remotes = createRemoteRepositories(repositories)

        val manager =
            ArtifactRepositoryManager(
                JarRepositoryManager.getLocalRepositoryPath(),
                remotes,
                object : ProgressConsumer {
                    override fun consume(@ProgressText message: @ProgressText String?) {
                        if (message != null) {
                            logger.debug(message)
                        }
                    }

                    override fun isCanceled(): Boolean = indicator?.isCanceled ?: false
                },
            )

        val resolvedFiles = mutableMapOf<String, File>()
        if (shouldDownload) {
            doDownload(description, version, exclusions, manager, resolvedFiles)
        }

        logger.debug("Collecting dependencies for ${description.getMavenCoordinates(version)}")
        val rootNode = manager.collectDependencies(description.groupId, description.artifactId, version)
        return rootNode to resolvedFiles
    }

    private fun doDownload(
        description: RepositoryLibraryDescription,
        version: String,
        exclusions: List<String>,
        manager: ArtifactRepositoryManager,
        resolvedFiles: MutableMap<String, File>,
    ) {
        @Suppress("TooGenericExceptionCaught") // IJPL throws generic exceptions :(
        try {
            logger.debug(
                "Resolving dependencies (downloading jars) for ${description.getMavenCoordinates(version)}"
            )
            val allExclusions = ProvidedDependencies.getExclusions() + exclusions
            val artifacts =
                manager.resolveDependencyAsArtifact(
                    description.groupId,
                    description.artifactId,
                    version,
                    EnumSet.of(ArtifactKind.ARTIFACT),
                    true,
                    allExclusions,
                )
            for (artifact in artifacts) {
                val coord = "${artifact.groupId}:${artifact.artifactId}:${artifact.version}"
                val file = artifact.file
                if (file != null) {
                    resolvedFiles[coord] = file
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to resolve dependencies for ${description.getMavenCoordinates(version)}", e)
        }
    }

    /**
     * Creates a list of [ArtifactRepositoryManager]'s remote repositories from the given [repositoryDescriptions].
     *
     * Inlined from `JarRepositoryManager.AetherJob.createRemoteRepositories`.
     */
    private fun createRemoteRepositories(repositoryDescriptions: MutableCollection<RemoteRepositoryDescription>) =
        buildList {
            for (repository in repositoryDescriptions) {
                val authData = obtainAuthenticationData(repository)
                add(
                    ArtifactRepositoryManager.createRemoteRepository(
                        repository.id,
                        repository.url,
                        authData,
                        repository.isAllowSnapshots,
                    )
                )
            }
        }

    /**
     * Obtains authentication data for the given remote repository description.
     *
     * Inlined from `JarRepositoryAuthenticationDataProvider.obtainAuthenticationData` to allow for easier access and to
     * avoid potential issues with internal APIs in different IDE versions.
     *
     * @param description the description of the remote repository
     * @return the authentication data, or null if none is found
     */
    @Suppress("UnstableApiUsage")
    @RequiresBackgroundThread
    private fun obtainAuthenticationData(
        description: RemoteRepositoryDescription,
    ): ArtifactRepositoryManager.ArtifactAuthenticationData? {
        for (extension in JarRepositoryAuthenticationDataProvider.KEY.extensionList) {
            val authData = extension.provideAuthenticationData(description.url)
            if (authData != null) {
                return ArtifactRepositoryManager.ArtifactAuthenticationData(authData.userName, authData.password)
            }
        }

        return null
    }

    /**
     * Converts an [ArtifactDependencyNode] tree to a printable string representation.
     */
    private fun ArtifactDependencyNode?.printableTreeString(indent: Int = 0): String = buildString {
        if (this@printableTreeString == null) {
            append("null")
            return@buildString
        }
        append(" ".repeat(indent))
        append("${artifact.groupId}:${artifact.artifactId}:${artifact.version}")
        if (artifact.classifier != null) append(":${artifact.classifier}")
        appendLine()
        dependencies.onEach { append(it.printableTreeString(indent + 2)) }
    }
}
