package io.gitlab.arturbosch.detekt.config

import io.gitlab.arturbosch.detekt.api.Config

/**
 * Config wrapper for disabling automatic correction.
 */
class NoAutoCorrectConfig(private val config: Config) : Config by config {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> valueOrDefault(key: String, default: T): T {
        if ("autoCorrect" == key) {
            return false as T
        }
        return config.valueOrDefault(key, default)
    }
}
