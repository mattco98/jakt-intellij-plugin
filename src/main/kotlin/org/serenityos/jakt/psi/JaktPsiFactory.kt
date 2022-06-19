package org.serenityos.jakt.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.intellij.sdk.language.psi.JaktNamespaceDeclaration
import org.intellij.sdk.language.psi.JaktTopLevelDefinition
import org.serenityos.jakt.JaktFile

class JaktPsiFactory(private val project: Project) {
    private fun createFile(text: String, fileName: String = "dummy.jakt") = PsiFileFactory
        .getInstance(project)
        .createFileFromText(
            fileName,
            JaktFile.FileType,
            text,
        ) as JaktFile

    fun createDefinition(text: String): JaktTopLevelDefinition {
        return createFile(text).children.first() as JaktTopLevelDefinition
    }

    fun createIdentifier(name: String) = createNamespace(name).identifier

    private fun createNamespace(name: String) =
        createFromText<JaktNamespaceDeclaration>("namespace $name {}")
            ?: error("Failed to create namespace")

    private inline fun <reified T : PsiElement> createFromText(text: String): T? = createFile(text).descendantOfType()
}
