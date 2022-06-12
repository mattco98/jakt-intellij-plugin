package org.serenityos.jakt.plugin.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.plugin.JaktFile
import org.serenityos.jakt.utils.descendantOfTypeStrict

class JaktPsiFactory(private val project: Project) {
    fun createFile(text: String, fileName: String = "dummy.jakt") = PsiFileFactory
        .getInstance(project)
        .createFileFromText(
            fileName,
            JaktFile.Type,
            text,
        ) as JaktFile

    fun createStatement(text: String): JaktStatement {
        return createFromText<JaktFunctionDeclaration>("function a() {$text}")!!.block!!.children[1] as JaktStatement
    }

    fun createReturnStatement(target: JaktExpression): JaktReturnStatement {
        val statement = createStatement("return 1")
        (statement as JaktReturnStatement).children[1] = target
        return statement
    }

    fun createBlock(vararg children: JaktPsiElement): JaktBlock {
        val body = ";".repeat(children.size)
        val block = createFromText<JaktFunctionDeclaration>("function a() {$body}")!!.block!!
        children.forEachIndexed { index, child ->
            block.children[index] = child
        }
        return block
    }

    fun createIdentifier(name: String) = createNamespace(name).identifier

    fun createNamespace(name: String) =
        createFromText<JaktNamespaceDeclaration>("namespace $name {}")
            ?: error("Failed to create namespace")

    private inline fun <reified T : PsiElement> createFromText(text: String): T? =
        createFile(text).descendantOfTypeStrict()
}