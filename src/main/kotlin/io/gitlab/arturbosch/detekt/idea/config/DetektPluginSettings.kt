package io.gitlab.arturbosch.detekt.idea.config

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.io.File

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

    var configurationFilePaths: MutableSet<String>
        get() = state.configurationFilePaths
        set(value) {
            state.configurationFilePaths = value
        }

    var baselinePath: String
        get() = state.baselinePath.orEmpty()
        set(value) {
            state.baselinePath = value
        }

    var pluginJarPaths: MutableSet<String>
        get() = state.pluginJarPaths
        set(value) {
            state.pluginJarPaths = value
        }

    override fun loadState(state: State) {
        val migrated = loadOrMigrateIfNeeded(state)
        super.loadState(migrated)
    }

    private fun loadOrMigrateIfNeeded(state: State): State {
        val migrationSettings = project.service<DetektSettingsMigration>()
        if (migrationSettings.state.stateVersion == CURRENT_VERSION) return state

        @Suppress("Deprecation") // TODO remove the migration from v0 settings storage at some point
        val oldSettings = DetektConfigStorage.instance(project)
        val migrated = State().apply {
            enableDetekt = oldSettings.enableDetekt
            enableFormatting = oldSettings.enableFormatting
            buildUponDefaultConfig = oldSettings.buildUponDefaultConfig
            enableAllRules = oldSettings.enableAllRules
            configurationFilePaths = oldSettings.configPaths
                .split(File.pathSeparator)
                .filter { it.isNotBlank() }
                .toMutableSet()
            pluginJarPaths = oldSettings.pluginPaths
                .split(File.pathSeparator)
                .filter { it.isNotBlank() }
                .toMutableSet()
            baselinePath = oldSettings.baselinePath
        }

        migrationSettings.state.stateVersion = CURRENT_VERSION
        return migrated
    }

    class State : BaseState() {

        var enableDetekt by property(true)
        var enableFormatting by property(false)
        var enableAllRules by property(false)
        var buildUponDefaultConfig by property(true)
        var treatAsErrors by property(false)

        var configurationFilePaths by stringSet()
        var baselinePath by string()
        var pluginJarPaths by stringSet()
    }

    companion object {

        private const val CURRENT_VERSION = 2
    }
}
