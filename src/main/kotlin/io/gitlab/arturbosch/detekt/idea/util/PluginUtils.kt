package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.util.IconLoader
import io.gitlab.arturbosch.detekt.idea.DETEKT
import javax.swing.Icon

object PluginUtils {

    fun pluginIcon(): Icon = IconLoader.getIcon("META-INF/pluginIconSmall.svg", PluginUtils::class.java.classLoader)

    fun platformVersion(): String = ApplicationInfo.getInstance().fullVersion

    fun pluginVersion(): String {
        val pluginId = PluginId.getId(DETEKT)
        val plugin = requireNotNull(PluginManagerCore.getPlugin(pluginId)) {
            "Could not find detekt plugin descriptor."
        }
        return plugin.version
    }

    fun isAtLeastIJBuild(minBuild: String): Boolean =
        ApplicationInfo.getInstance().build > BuildNumber.fromString(minBuild)!!
}
