package io.gitlab.arturbosch.detekt.util

import com.intellij.psi.PsiFile
import com.intellij.util.io.write
import java.nio.file.Files
import java.nio.file.Path

private val tmpDir = Files.createTempDirectory("detekt.intellij.plugin")

fun PsiFile.toTmpFile(): Path {
    if (Files.notExists(tmpDir)) {
        Files.createDirectory(tmpDir)
    }
    val tmpFile = tmpDir.resolve(this.virtualFile.name)
    tmpFile.write(this.text.toByteArray())
    return tmpFile
}
