package io.gitlab.arturbosch.detekt.idea

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.MockFileTypeManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.impl.PsiManagerImpl
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import io.gitlab.arturbosch.detekt.idea.config.DetektSettingsMigration

fun mockProject(basePath: String? = null): MockProject =
    object : MockProject(null, {}) {
        override fun isDefault(): Boolean = true
        override fun getBasePath(): String? = basePath
    }.apply {
        registerService(DetektPluginSettings::class.java)
        registerService(DetektSettingsMigration::class.java)
        registerService(
            io.gitlab.arturbosch.detekt.idea.util.PluginDependencyService::class.java,
            io.gitlab.arturbosch.detekt.idea.util.PluginDependencyService(this),
        )
    }

fun heavyMockProject(rootDisposable: Disposable, basePath: String? = null): MockProject =
    mockProject(basePath).apply {
        val env = CoreApplicationEnvironment(rootDisposable, true)
        val application = env.application
        val typeManager = MockFileTypeManager()
        application.registerService(FileTypeRegistry::class.java, typeManager)
        application.registerService(FileTypeManager::class.java, typeManager)
        ApplicationManager.setApplication(application, { FileTypeManager.getInstance() }, rootDisposable)

        val manager = PsiManagerImpl(this)
        registerService(PsiManager::class.java, manager)
        registerService(PsiFileFactory::class.java, PsiFileFactoryImpl(manager))
    }
