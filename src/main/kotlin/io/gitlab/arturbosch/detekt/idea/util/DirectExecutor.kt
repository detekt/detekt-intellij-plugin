package io.gitlab.arturbosch.detekt.idea.util

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class DirectExecutor : AbstractExecutorService() {

    override fun isTerminated(): Boolean = true

    override fun execute(command: Runnable) {
        command.run()
    }

    override fun shutdown() {
        // NOOP
    }

    override fun shutdownNow(): MutableList<Runnable> = mutableListOf()

    override fun isShutdown(): Boolean = true

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
}
