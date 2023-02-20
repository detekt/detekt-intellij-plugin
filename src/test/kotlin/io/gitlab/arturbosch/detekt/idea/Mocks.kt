package io.gitlab.arturbosch.detekt.idea

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings

fun mockProject(): Project = object : MockProject(null, { }) {
    override fun isDefault(): Boolean = true
}.apply {
    registerService(DetektPluginSettings::class.java)
}
