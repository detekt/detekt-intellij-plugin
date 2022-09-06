package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import io.gitlab.arturbosch.detekt.idea.config.ui.DescriptorAssert.Companion.assertThat
import org.assertj.core.api.AbstractAssert
import org.junit.jupiter.api.Test

class FileChooserDescriptorUtilTest {

    @Test
    fun `yaml chooser should select yml and yaml files`() {
        val descriptor = FileChooserDescriptorUtil.createYamlChooserDescriptor()
        assertThat(descriptor).isFileSelectable("detekt.yml")
        assertThat(descriptor).isFileSelectable("detekt.yaml")
        assertThat(descriptor).isFileSelectable("DETEKT.YML")
        assertThat(descriptor).isFileSelectable("DETEKT.YAML")
    }

}

private class DescriptorAssert(actual: FileChooserDescriptor) :
    AbstractAssert<DescriptorAssert, FileChooserDescriptor>(actual, DescriptorAssert::class.java) {

    companion object {
        fun assertThat(actual: FileChooserDescriptor) = DescriptorAssert(actual)
    }

    fun isFileSelectable(name: String): DescriptorAssert {
        if (!actual.isFileSelectable(MockVirtualFile.file(name))) {
            failWithMessage("Expected file <$name> to be selectable.")
        }
        return this
    }

}
