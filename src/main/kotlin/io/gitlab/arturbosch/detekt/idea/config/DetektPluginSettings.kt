package io.gitlab.arturbosch.detekt.idea.config

import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "DetektPluginSettings", storages = [Storage("detekt.xml")])
class DetektPluginSettings(
    private val project: Project
) : SimplePersistentStateComponent<DetektPluginSettings.State>(State()) {

    var enableDetekt: Boolean
        get() = state.enableDetekt
        set(value) {
            state.enableDetekt = value
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

    override fun loadState(state: State) {
        val migrated = loadOrMigrateIfNeeded(state)
        super.loadState(migrated)
    }

    private fun loadOrMigrateIfNeeded(state: State): State {
        val migrationSettings = project.service<DetektSettingsMigration>()

        return when (val stateVersion = migrationSettings.stateVersion) {
            DetektSettingsMigration.CURRENT_VERSION -> state
            2 -> migrationSettings.migrateFromV2ToCurrent(state)
            1 -> migrationSettings.migrateFromV1ToCurrent()
            else -> {
                thisLogger().error("Unsupported settings value, cannot migrate: $stateVersion. Resetting to defaults.")
                State()
            }
        }
    }

    class State : BaseState() {

        var enableDetekt by property(true)
        var enableFormatting by property(false)
        var enableAllRules by property(false)
        var buildUponDefaultConfig by property(true)
        var treatAsErrors by property(false)

        var configurationFiles by list<String>()

        var baselinePath by string()
        var pluginJars by list<String>()

        //////////////////////////////////
        //// Deprecated v2 properties ////
        //////////////////////////////////
        @Deprecated("Migrated to configurationFiles", ReplaceWith("configurationFiles"))
        var configurationFilePaths by stringSet()

        @Deprecated("Migrated to pluginJars", ReplaceWith("pluginJars"))
        var pluginJarPaths by stringSet()
    }
}
