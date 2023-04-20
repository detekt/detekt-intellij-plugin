package io.gitlab.arturbosch.detekt.idea.util

class SimpleAppendable(private val consumer: (String) -> Unit) : Appendable {

    override fun append(csq: CharSequence): Appendable {
        val text = csq.toString().trim()
        if (text.isNotEmpty()) {
            consumer.invoke(text)
        }
        return this
    }

    override fun append(csq: CharSequence, start: Int, end: Int): Appendable {
        append(csq.subSequence(start, end))
        return this
    }

    override fun append(c: Char): Appendable {
        append(c.toString())
        return this
    }
}
