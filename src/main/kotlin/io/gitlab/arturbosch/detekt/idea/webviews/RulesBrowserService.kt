package io.gitlab.arturbosch.detekt.idea.webviews

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.applyIf
import com.intellij.util.ui.StartupUiUtil
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.JComponent

@Service
class RulesBrowserService : Disposable {

    companion object {
        fun getInstance(): RulesBrowserService = service()
    }

    private val isIdeWithFocusProblems = ApplicationInfo.getInstance().shortVersion == "2022.3"

    private val browser = JBCefBrowser.createBuilder()
        .setUrl(DocsPage.Rules.url)
        .applyIf(isIdeWithFocusProblems) { setOffScreenRendering(true) }
        .build()
        .apply {
            jbCefClient.addLoadHandler(
                object : CefLoadHandlerAdapter() {
                    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                        updateStyles()
                    }
                },
                this.cefBrowser,
            )
        }

    init {
        val connect = ApplicationManager.getApplication().messageBus.connect()
        connect.subscribe(
            LafManagerListener.TOPIC,
            LafManagerListener {
                updateStyles()
            },
        )
        Disposer.register(this, connect)
    }

    fun getComponent(): JComponent = browser.component

    fun loadPage(page: DocsPage) {
        browser.loadURL(page.url)
    }

    private fun updateStyles() {
        val ideTheme = if (StartupUiUtil.isUnderDarcula()) "dark" else "light"
        browser.cefBrowser.executeJavaScript(
            """
                if (localStorage.theme !== "$ideTheme") {
                    (
                        document.querySelector('.navbar-sidebar__brand button[class*="toggle"]')
                        ?? document.querySelector('.navbar div[class*="colorModeToggle"] button')
                    )
                    ?.click();
                }
            """.trimIndent(),
            "about:blank",
            0,
        )
    }

    override fun dispose() {
        browser.dispose()
    }
}
