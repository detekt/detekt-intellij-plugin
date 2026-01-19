package io.gitlab.arturbosch.detekt.idea.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import io.gitlab.arturbosch.detekt.idea.util.DetektPlugin
import io.gitlab.arturbosch.detekt.idea.util.PluginDependencyService

@Service(Service.Level.PROJECT)
@State(name = "DetektPluginSettings", storages = [Storage("detekt.xml")])
class DetektPluginSettings(
    private val project: Project,
) : SimplePersistentStateComponent<DetektPluginSettings.State>(State()) {

    var enableDetekt: Boolean
        get() = state.enableDetekt
        set(value) {
            state.enableDetekt = value
        }

    val shouldShowPromptToEnable: Boolean
        get() = state.enableForProjectResult == EnableForProjectPromptResult.NotSet

    var enableForProjectResult: EnableForProjectPromptResult
        get() = state.enableForProjectResult
        set(value) {
            state.enableForProjectResult = value
        }

    var enableFormatting: Boolean
        get() = state.enableFormatting
        set(value) {
            state.enableFormatting = value
        }

    var buildUponDefaultConfig: Boolean
        get() = state.buildUponDefaultConfig
        set(value) {
            state.buildUponDefaultConfig = value
        }

    var enableAllRules: Boolean
        get() = state.enableAllRules
        set(value) {
            state.enableAllRules = value
        }

    var treatAsErrors: Boolean
        get() = state.treatAsErrors
        set(value) {
            state.treatAsErrors = value
        }

    var configurationFilePaths: List<String>
        get() = state.configurationFiles
        set(value) {
            state.configurationFiles = value.toMutableList()
        }

    var baselinePath: String
        get() = state.baselinePath.orEmpty()
        set(value) {
            state.baselinePath = value
        }

    var pluginJarPaths: List<String>
        get() = state.pluginJars
        set(value) {
            state.pluginJars = value.toMutableList()
        }

    var plugins: List<DetektPlugin>
        get() = state.plugins
        set(value) {
            state.plugins = value.toMutableList()
        }

    var debug: Boolean
        get() = state.detektDebugMode
        set(value) {
            state.detektDebugMode = value
        }

    var redirectChannels: Boolean
        get() = state.redirectChannels
        set(value) {
            state.redirectChannels = value
        }

    override fun loadState(state: State) {
        val migrated = loadOrMigrateIfNeeded(state)
        super.loadState(migrated)
        val app = ApplicationManager.getApplication()
        if (app != null && !app.isUnitTestMode) {
            // May run close in time to DetektConfig.apply(); PluginDependencyService serializes both via a lock
            // so only one reconciliation runs at a time and on-disk state stays consistent.
            project.service<PluginDependencyService>().reconcilePlugins(migrated.plugins)
        }
    }

    private fun loadOrMigrateIfNeeded(state: State): State {
        val migrationSettings = project.service<DetektSettingsMigration>()

        return when (val stateVersion = migrationSettings.stateVersion) {
            DetektSettingsMigration.CURRENT_VERSION -> state
            2 -> migrationSettings.migrateFromV2ToCurrent(state)
            else -> {
                thisLogger().error("Unsupported settings value, cannot migrate: $stateVersion. Resetting to defaults.")
                State()
            }
        }
    }

    class State : BaseState() {

        var redirectChannels by property(false)
        var detektDebugMode by property(false)
        var enableDetekt by property(false)
        var enableFormatting by property(false)
        var enableAllRules by property(false)
        var buildUponDefaultConfig by property(true)
        var treatAsErrors by property(false)
        var enableForProjectResult by enum(EnableForProjectPromptResult.NotSet)

        var configurationFiles by list<String>()

        var baselinePath by string()
        var pluginJars by list<String>()
        var plugins by list<DetektPlugin>()

        @Suppress("CommentSpacing")
        //////////////////////////////////
        //// Deprecated v2 properties ////
        //////////////////////////////////
        @Deprecated("Migrated to configurationFiles", ReplaceWith("configurationFiles"))
        var configurationFilePaths by stringSet()

        @Deprecated("Migrated to pluginJars", ReplaceWith("pluginJars"))
        var pluginJarPaths by stringSet()
    }

    enum class EnableForProjectPromptResult {
        NotSet,
        Declined,
        Accepted,
    }
}
