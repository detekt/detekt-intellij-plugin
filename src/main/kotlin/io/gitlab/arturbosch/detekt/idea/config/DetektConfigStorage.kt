package io.gitlab.arturbosch.detekt.idea.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Tag

@State(
    name = "DetektProjectConfiguration",
    storages = [Storage("detekt.xml")]
)
class DetektConfigStorage : PersistentStateComponent<DetektConfigStorage> {

    @Tag
    var enableDetekt: Boolean = false

    @Tag
    var enableFormatting: Boolean = false

    @Tag
    var buildUponDefaultConfig: Boolean = false

    @Tag
    var enableAllRules: Boolean = false

    @Tag
    var treatAsError: Boolean = false

    @Tag
    var rulesPath: String = ""

    @Tag
    var baselinePath: String = ""

    @Tag
    var pluginPaths: String = ""

    override fun getState(): DetektConfigStorage = this

    override fun loadState(state: DetektConfigStorage) {
        this.enableDetekt = state.enableDetekt
        this.enableFormatting = state.enableFormatting
        this.buildUponDefaultConfig = state.buildUponDefaultConfig
        this.enableAllRules = state.enableAllRules
        this.rulesPath = state.rulesPath
        this.baselinePath = state.baselinePath
        this.treatAsError = state.treatAsError
        this.pluginPaths = state.pluginPaths
    }

    companion object {

        /**
         * Get instance of [DetektConfigStorage] for given project.
         *
         * @param project the project
         */
        fun instance(project: Project): DetektConfigStorage =
            ServiceManager.getService(project, DetektConfigStorage::class.java)
    }
}
