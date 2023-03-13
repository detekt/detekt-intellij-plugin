package io.gitlab.arturbosch.detekt.idea.webviews

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.ui.jcef.JBCefBrowser
import javax.swing.JComponent

@Service
class RulesBrowserService : Disposable {

    companion object {
        fun getInstance(): RulesBrowserService = service()
    }

    private val browser by lazy { JBCefBrowser() }

    fun getComponent(): JComponent = browser.component

    fun loadRulesPage() {
        browser.loadURL("https://detekt.dev/docs/rules/style")
    }

    fun loadChangelog() {
        browser.loadURL("https://detekt.dev/changelog")
    }

    override fun dispose() {
        browser.dispose()
    }
}
