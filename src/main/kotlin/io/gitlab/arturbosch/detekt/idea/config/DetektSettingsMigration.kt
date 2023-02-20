package io.gitlab.arturbosch.detekt.idea.config

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@Service(Service.Level.PROJECT)
@State(name = "DetektSettingsMigration", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
internal class DetektSettingsMigration : SimplePersistentStateComponent<DetektSettingsMigration.State>(State()) {

    // ---------
    // Changelog
    // ---------
    //
    // v3 changed sets to lists to retain order
    // v2 allowed multiple config files and plugin jars using sets
    // v1 initial version - removed in 1.22.3+
    var stateVersion
        get() = state.stateVersion.takeIf { it > 0 } ?: CURRENT_VERSION
        set(value) {
            state.stateVersion = value
        }

    @Suppress("Deprecation") // TODO remove the migration from v2 settings storage at some point
    fun migrateFromV2ToCurrent(v2State: DetektPluginSettings.State): DetektPluginSettings.State {
        val migrated = DetektPluginSettings.State().apply {
            copyFrom(v2State)
            pluginJars = pluginJarPaths.toMutableList()
            pluginJarPaths.clear()
            configurationFiles = configurationFilePaths.toMutableList()
            configurationFilePaths.clear()
        }

        state.stateVersion = CURRENT_VERSION
        return migrated
    }

    class State : BaseState() {

        var stateVersion by property(-1)
    }

    companion object {

        const val CURRENT_VERSION = 3
    }
}
