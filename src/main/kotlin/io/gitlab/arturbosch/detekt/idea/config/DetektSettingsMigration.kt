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

    class State : BaseState() {

        var stateVersion by property(0)
    }
}
