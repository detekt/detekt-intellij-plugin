package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.scale.JBUIScale
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.AsyncProcessIcon
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.util.DetektPlugin
import io.gitlab.arturbosch.detekt.idea.util.MavenDependencyResolver
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.table.DefaultTableModel

private const val SEARCH_DIALOG_WIDTH = 600
private const val SEARCH_DIALOG_HEIGHT = 450
private const val VERSION_COMBO_WIDTH = 200

/**
 * Dialog for searching Maven artifacts. It allows users to search for artifacts, select a version, and customize
 * transitive dependencies.
 */
@Suppress("TooManyFunctions") // Unavoidable due to UI DSL
class MavenSearchDialog(
    private val project: Project,
    private val artifactFetcher: MavenArtifactFetcher = MavenArtifactFetcher(project),
    private val dependencyResolver: MavenDependencyResolver = MavenDependencyResolver(project),
) : DialogWrapper(project) {

    private val logger = thisLogger()
    private val application = ApplicationManager.getApplication()

    private var pendingGav: MavenArtifact.MavenGav? = null

    private lateinit var searchField: JBTextField
    private lateinit var searchButton: JButton
    private lateinit var resultTable: JBTable
    private lateinit var versionComboBox: ComboBox<String>

    // Transitive Deps UI
    private lateinit var transitiveDepsLabel: JLabel
    private lateinit var customizeLink: ActionLink
    private lateinit var transitiveProcessIcon: AsyncProcessIcon

    private val resultModel =
        object :
            DefaultTableModel(
                arrayOf(
                    DetektBundle.message("detekt.configuration.mavenSearch.tableHeader.group"),
                    DetektBundle.message("detekt.configuration.mavenSearch.tableHeader.artifact"),
                    DetektBundle.message("detekt.configuration.mavenSearch.tableHeader.latestVersion"),
                ),
                0,
            ) {
            override fun isCellEditable(row: Int, column: Int): Boolean = false
        }

    private var searchResults: List<MavenArtifact> = emptyList()
    private var selectedArtifact: MavenArtifact? = null
    private var isSearching = false

    // State for the selected artifact
    private var isResolving: Boolean = false // simple flag to track if we are resolving
    private var resolvedRoot: MavenDependencyResolver.ResolvedNode? = null
    private var currentExclusions: MutableSet<String> = mutableSetOf()

    init {
        title = DetektBundle.message("detekt.configuration.mavenSearch.title")
        init()
        isOKActionEnabled = false
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            searchBarRow()
            marketplaceRow()
            resultsRow()
            versionsRow()
            transitiveDependenciesRow()
        }

        panel.preferredSize = Dimension(SEARCH_DIALOG_WIDTH, SEARCH_DIALOG_HEIGHT)

        return panel
    }

    private fun Panel.marketplaceRow() {
        row {
            comment(DetektBundle.message("detekt.configuration.mavenSearch.marketplaceBlurb"))
        }
    }

    private fun Panel.searchBarRow() {
        row(DetektBundle.message("detekt.configuration.mavenSearch.searchLabel")) {
            searchField = textField()
                .align(AlignX.FILL)
                .resizableColumn()
                .applyToComponent { addActionListener { performSearch() } }
                .component

            searchButton = button(DetektBundle.message("detekt.configuration.mavenSearch.searchButton")) {
                performSearch()
            }.applyToComponent {
                isEnabled = false
                registerKeyboardAction(
                    { performSearch() },
                    KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                    JComponent.WHEN_FOCUSED,
                )
            }.component

            searchField.document.addDocumentListener(
                object : DocumentAdapter() {
                    override fun textChanged(e: DocumentEvent) {
                        updateSearchButtonState()
                    }
                }
            )
        }
    }

    private fun Panel.resultsRow() {
        resultTable = JBTable(resultModel)
        resultTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        resultTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                onArtifactSelected()
            }
        }

        val mouseAdapter =
            object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) = showPopupMenu(e)

                override fun mouseReleased(e: MouseEvent) = showPopupMenu(e)

                private fun showPopupMenu(e: MouseEvent) {
                    if (e.isPopupTrigger) {
                        val row = resultTable.rowAtPoint(e.point)
                        if (row != -1) {
                            resultTable.setRowSelectionInterval(row, row)
                            createContextMenu(row).show(e.component, e.x, e.y)
                        }
                    }
                }
            }
        resultTable.addMouseListener(mouseAdapter)

        TableSpeedSearch.installOn(resultTable)

        row {
            // Wrap the table in a scroll pane to prevent the table from growing too large
            cell(JBScrollPane(resultTable)).align(AlignX.FILL).align(AlignY.FILL)
        }.resizableRow()
    }

    private fun Panel.versionsRow() {
        row(DetektBundle.message("detekt.configuration.mavenSearch.versionLabel")) {
            versionComboBox = comboBox(DefaultComboBoxModel<String>())
                .applyToComponent {
                    isEnabled = false
                    preferredSize = Dimension(JBUIScale.scale(VERSION_COMBO_WIDTH), preferredSize.height)
                    addActionListener {
                        // re-resolve on version change
                        if (isEnabled) resolveDependencies()
                    }
                }.component
        }
    }

    private fun Panel.transitiveDependenciesRow() {
        row {
            transitiveProcessIcon = cell(AsyncProcessIcon("TransitiveResolution"))
                .applyToComponent { isVisible = false }
                .component

            transitiveDepsLabel = label("").component

            customizeLink = link(DetektBundle.message("detekt.configuration.mavenSearch.customize")) {
                openCustomizeDialog()
            }.applyToComponent { isVisible = false }
                .component
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = searchField

    /** Returns the selected artifact as a DetektPlugin object. */
    fun getResult(): DetektPlugin? {
        val artifact = selectedArtifact
        val version = versionComboBox.selectedItem as? String
        if (artifact == null || version == null) return null

        val coordinate = "${artifact.groupId}:${artifact.artifactId}:$version"
        return DetektPlugin(coordinate, currentExclusions)
    }

    private fun updateSearchButtonState() {
        searchButton.isEnabled = !isSearching && searchField.text.isNotBlank()
    }

    private fun performSearch() {
        val query = searchField.text.trim()
        if (query.isEmpty()) return

        logger.debug("Performing Maven search for: $query")

        val gav = MavenArtifactFetcher.parseGav(query)
        if (gav != null) {
            pendingGav = gav
            val searchQuery = gav.artifactId
            logger.debug(
                "Detected GAV coordinate. Searching for artifactId: $searchQuery (target version: ${gav.version})"
            )
            startSearchUI()
            artifactFetcher.searchArtifacts(searchQuery) { results ->
                application.invokeLater({ showResults(results) }, ModalityState.any())
            }
        } else {
            pendingGav = null
            startSearchUI()
            artifactFetcher.searchArtifacts(query) { results ->
                application.invokeLater({ showResults(results) }, ModalityState.any())
            }
        }
    }

    private fun startSearchUI() {
        resultModel.rowCount = 0
        searchResults = emptyList()
        selectedArtifact = null
        versionComboBox.removeAllItems()
        versionComboBox.isEnabled = false
        isSearching = true
        updateSearchButtonState()
        resultTable.setPaintBusy(true)
        resultTable.emptyText.text = com.intellij.util.ui.StatusText.getDefaultEmptyText()

        // Reset transitive area
        resetTransitiveUI()
    }

    private fun resetTransitiveUI() {
        isOKActionEnabled = false
        transitiveDepsLabel.text = ""
        customizeLink.isVisible = false
        resolvedRoot = null
        currentExclusions.clear()
        isResolving = false
        transitiveProcessIcon.isVisible = false
        transitiveProcessIcon.suspend()
    }

    private fun showResults(artifacts: List<MavenArtifact>) {
        logger.debug("Showing ${artifacts.size} results")

        val target = pendingGav
        val filteredArtifacts =
            if (target != null) {
                artifacts.filter { it.groupId == target.groupId && it.artifactId == target.artifactId }
            } else {
                artifacts
            }

        searchResults = filteredArtifacts
        resultModel.rowCount = 0

        for (artifact in filteredArtifacts) {
            resultModel.addRow(arrayOf(artifact.groupId, artifact.artifactId, artifact.latestVersion ?: ""))
        }

        if (filteredArtifacts.isEmpty()) {
            resultTable.emptyText.text = DetektBundle.message("detekt.configuration.mavenSearch.noResults")
        }

        isSearching = false
        updateSearchButtonState()
        resultTable.setPaintBusy(false)

        if (target != null && filteredArtifacts.isNotEmpty()) {
            // Auto-select the first (and likely only) matching result
            logger.debug("Auto-selecting artifact row for GAV: ${target.groupId}:${target.artifactId}")
            resultTable.setRowSelectionInterval(0, 0)
            onArtifactSelected() // Explicitly trigger to ensure versions are shown
        }
    }

    private fun onArtifactSelected() {
        val row = resultTable.selectedRow
        if (row >= 0 && row < searchResults.size) {
            val artifact = searchResults[row]
            selectedArtifact = artifact
            logger.debug("Selected artifact: ${artifact.groupId}:${artifact.artifactId}")

            val targetVersion =
                pendingGav?.let {
                    if (it.groupId == artifact.groupId && it.artifactId == artifact.artifactId) it.version else null
                }
            showVersions(artifact.versions, targetVersion)

            if (targetVersion != null) {
                pendingGav = null
            }

            // showVersions will trigger action listener which calls resolveDependencies
            // But we need to make sure we call it if the list is populated and index 0 selected
            // automatically
            if (versionComboBox.itemCount > 0) {
                resolveDependencies()
            }
        } else {
            selectedArtifact = null
            versionComboBox.removeAllItems()
            versionComboBox.isEnabled = false
            resetTransitiveUI()
        }
    }

    private fun showVersions(versions: List<String>, targetVersion: String? = null) {
        versionComboBox.removeAllItems()
        versions.forEach { versionComboBox.addItem(it) }

        if (targetVersion != null && versions.contains(targetVersion)) {
            logger.debug("Auto-selecting target version: $targetVersion")
            versionComboBox.selectedItem = targetVersion
        } else if (versions.isNotEmpty()) {
            versionComboBox.selectedIndex = 0
        }
        versionComboBox.isEnabled = versions.isNotEmpty()
    }

    private fun resolveDependencies() {
        val artifact = selectedArtifact ?: return
        val version = versionComboBox.selectedItem as? String ?: return

        logger.debug("Resolving dependencies for ${artifact.groupId}:${artifact.artifactId}:$version")

        // UI State: Loading
        isOKActionEnabled = false
        isResolving = true
        transitiveDepsLabel.text = DetektBundle.message("detekt.configuration.mavenSearch.resolvingTransitive", version)
        customizeLink.isVisible = false
        transitiveProcessIcon.isVisible = true
        transitiveProcessIcon.resume()

        val coordinate = "${artifact.groupId}:${artifact.artifactId}:$version"

        application.executeOnPooledThread {
            @Suppress("TooGenericExceptionCaught") // IJPL throws generic exceptions :(
            try {
                val root = dependencyResolver.resolve(coordinate)
                application.invokeLater({ onDependenciesResolved(root) }, ModalityState.any())
            } catch (e: Exception) {
                logger.error("Failed to resolve dependencies for $coordinate", e)
                application.invokeLater({ onDependenciesResolved(null) }, ModalityState.any())
            }
        }
    }

    private fun onDependenciesResolved(root: MavenDependencyResolver.ResolvedNode?) {
        if (!isResolving) return

        isResolving = false
        transitiveProcessIcon.isVisible = false
        transitiveProcessIcon.suspend()

        resolvedRoot = root
        currentExclusions.clear()

        if (root != null) {
            logger.debug("Successfully resolved dependencies for ${root.coordinate}")
            isOKActionEnabled = true
            updateTransitiveLabel()
            customizeLink.isVisible = true
        } else {
            logger.warn("Failed to resolve dependencies")
            transitiveDepsLabel.text = DetektBundle.message("detekt.configuration.mavenSearch.failedToResolve")
            isOKActionEnabled = false
            customizeLink.isVisible = false
        }
    }

    private fun updateTransitiveLabel() {
        val root = resolvedRoot ?: return

        val allNodes = flatten(root)
        val validDeps =
            allNodes.filter {
                it.coordinate != root.coordinate && !it.isProvided && it.coordinate !in currentExclusions
            }

        val count = validDeps.distinctBy { it.coordinate }.size
        transitiveDepsLabel.text = DetektBundle.message("detekt.configuration.mavenSearch.includesTransitive", count)
    }

    private fun flatten(node: MavenDependencyResolver.ResolvedNode): List<MavenDependencyResolver.ResolvedNode> =
        listOf(node) + node.children.flatMap { flatten(it) }

    private fun openCustomizeDialog() {
        val root = resolvedRoot ?: return
        val editor = DependencyExclusionEditor(root, contentPane)
        val newExclusions = editor.selectExcludedDependencies(currentExclusions)
        if (newExclusions != null) {
            currentExclusions.clear()
            currentExclusions.addAll(newExclusions)
            updateTransitiveLabel()
        }
    }

    private fun createContextMenu(row: Int): JPopupMenu {
        val popup = JPopupMenu()
        val copyItem = JMenuItem(DetektBundle.message("detekt.configuration.mavenSearch.copyCoordinates"))
        copyItem.addActionListener {
            val artifact = searchResults[row]
            val coords = "${artifact.groupId}:${artifact.artifactId}"
            CopyPasteManager.getInstance().setContents(StringSelection(coords))
        }
        popup.add(copyItem)
        return popup
    }
}
