package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.openapi.ui.DialogPanel

internal interface DetektConfigUiProvider {

    fun createPanel(): DialogPanel
}
