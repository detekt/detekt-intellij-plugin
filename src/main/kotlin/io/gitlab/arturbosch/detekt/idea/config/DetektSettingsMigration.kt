package io.gitlab.arturbosch.detekt.idea.config

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import java.io.File

@Service(Service.Level.PROJECT)
@State(name = "DetektSettingsMigration", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
internal class DetektSettingsMigration(private val project: Project) :
    SimplePersistentStateComponent<DetektSettingsMigration.State>(State()) {

    // ---------
    // Changelog
    // ---------
    //
    // v3 changed sets to lists to retain order
    // v2 allowed multiple config files and plugin jars using sets
    // v1 initial version
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

    fun migrateFromV1ToCurrent(): DetektPluginSettings.State {
        @Suppress("Deprecation") // TODO remove the migration from v1 settings storage at some point
        val v1ConfigStorage = DetektConfigStorage.instance(project)

        val migrated = DetektPluginSettings.State().apply {
            enableDetekt = v1ConfigStorage.enableDetekt
            enableFormatting = v1ConfigStorage.enableFormatting
            buildUponDefaultConfig = v1ConfigStorage.buildUponDefaultConfig
            enableAllRules = v1ConfigStorage.enableAllRules
            configurationFiles = migrateV1Paths(v1ConfigStorage.configPaths)
            pluginJars = migrateV1Paths(v1ConfigStorage.pluginPaths)
            baselinePath = v1ConfigStorage.baselinePath
        }

        state.stateVersion = CURRENT_VERSION
        return migrated
    }

    private fun migrateV1Paths(paths: String): MutableList<String> =
        paths.split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .toMutableList()

    class State : BaseState() {

        var stateVersion by property(-1)
    }

    companion object {

        const val CURRENT_VERSION = 3
    }
}
