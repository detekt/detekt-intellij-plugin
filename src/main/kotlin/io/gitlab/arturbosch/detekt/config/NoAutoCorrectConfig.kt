package io.gitlab.arturbosch.detekt.config

import io.gitlab.arturbosch.detekt.api.Config

/**
 * Config wrapper for disabling automatic correction if it is not globally activated.
 */
class NoAutoCorrectConfig(private val config: Config, private val globalAutocorrect: Boolean) : Config by config {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> valueOrDefault(key: String, default: T): T {
        if (!globalAutocorrect && "autoCorrect" == key) {
            return false as T
        }
        return config.valueOrDefault(key, default)
    }
}
