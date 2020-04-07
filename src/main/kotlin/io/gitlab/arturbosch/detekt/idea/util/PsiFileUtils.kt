package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiFile
import com.intellij.util.io.createDirectories
import com.intellij.util.io.write
import java.nio.file.Files
import java.nio.file.Path

private val tmpDir = Files.createTempDirectory("detekt-ip")

fun PsiFile.toTmpFile(): Path? {
    val base = this.project.basePath
    var path = this.virtualFile?.path
    if (base == null || path == null) {
        return null
    }
    if (Files.notExists(tmpDir)) {
        Files.createDirectory(tmpDir)
    }
    path = path.removePrefix("$base").removePrefix("/")
    val tmpFile = tmpDir.resolve(path)
    tmpFile.parent.createDirectories()
    val content = ReadAction.compute<ByteArray, Throwable> { this.text.toByteArray() }
    tmpFile.write(content)
    return tmpFile
}
