package org.serenityos.jakt

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.api.*

class JaktFoldingBuilder : CustomFoldingBuilder() {
    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean
    ) {
        val visitor = FoldingVisitor(descriptors)
        PsiTreeUtil.processElements(root) {
            it.accept(visitor)
            true
        }
    }

    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String {
        return "{ ... }"
    }

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean {
        return false
    }

    private class FoldingVisitor(private val descriptors: MutableList<FoldingDescriptor>) : JaktVisitor() {
        override fun visitPsiElement(o: JaktPsiElement) {
        }

        override fun visitFunction(o: JaktFunction) {
            val block = o.block ?: return
            descriptors += FoldingDescriptor(block)
        }

        override fun visitStructDeclaration(o: JaktStructDeclaration) {
            descriptors += FoldingDescriptor(o.structBody)
        }

        override fun visitEnumDeclaration(o: JaktEnumDeclaration) {
            descriptors += if (o.underlyingTypeEnumBody != null) {
                val body = o.underlyingTypeEnumBody!!
                FoldingDescriptor(o, TextRange(body.curlyOpen.startOffset, body.curlyClose.endOffset))
            } else {
                val body = o.normalEnumBody!!
                FoldingDescriptor(o, TextRange(body.curlyOpen.startOffset, body.curlyClose.endOffset))
            }
        }

        override fun visitNamespaceDeclaration(o: JaktNamespaceDeclaration) {
            val body = o.namespaceBody
            descriptors += FoldingDescriptor(o, TextRange(body.curlyOpen.startOffset, body.curlyClose.endOffset))
        }

        override fun visitBlock(o: JaktBlock) {
            descriptors += FoldingDescriptor(o, o.textRange)
        }

        private fun FoldingDescriptor(element: PsiElement) = FoldingDescriptor(element, element.textRange)
    }
}
