package org.serenityos.jakt.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.api.JaktBlock
import org.serenityos.jakt.psi.api.JaktNamespaceDeclaration
import org.serenityos.jakt.psi.api.JaktPlainQualifier
import org.serenityos.jakt.psi.api.JaktTopLevelDefinition

class JaktPsiFactory(private val project: Project) {
    fun createFile(text: String, fileName: String = "dummy.jakt") = PsiFileFactory
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

    fun createPlainQualifier(name: String): JaktPlainQualifier {
        val text = "function foo() { $name }"
        val file = createFile(text)
        return PsiTreeUtil.findChildOfType(file, JaktPlainQualifier::class.java)
            ?: error("Failed to create plain qualifier")
    }

    fun createBlock(contents: String): JaktBlock {
        val text = "function foo() { $contents }"
        val file = createFile(text)
        return PsiTreeUtil.findChildOfType(file, JaktBlock::class.java) ?: error("Failed to create block")
    }

    private fun createNamespace(name: String) =
        createFromText<JaktNamespaceDeclaration>("namespace $name {}")
            ?: error("Failed to create namespace")

    inline fun <reified T : PsiElement> createFromText(text: String): T? = createFile(text).descendantOfType()
}
