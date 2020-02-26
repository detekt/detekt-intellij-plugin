package io.gitlab.arturbosch.detekt.util

import com.intellij.psi.PsiFile
import com.intellij.util.io.createDirectories
import com.intellij.util.io.write
import java.nio.file.Files
import java.nio.file.Path

private val tmpDir = Files.createTempDirectory("detekt-ip")

fun PsiFile.toTmpFile(): Path? {
    val base = this.project.basePath
    val path = this.virtualFile?.path
    if (base == null || path == null) {
        return null
    }
    if (Files.notExists(tmpDir)) {
        Files.createDirectory(tmpDir)
    }
    val tmpFile = tmpDir.resolve(path.removePrefix("$base/"))
    tmpFile.parent.createDirectories()
    tmpFile.write(this.text.toByteArray())
    return tmpFile
}
