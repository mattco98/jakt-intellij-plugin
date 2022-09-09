package org.serenityos.jakt.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.api.JaktImport
import org.serenityos.jakt.psi.api.JaktImportBraceList
import org.serenityos.jakt.psi.api.JaktVisitor

class JaktImportFoldingBuilder : CustomFoldingBuilder() {

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
        if (node.psi is JaktImport) {
            val import = node.psi as JaktImport
            val importCount = import.importBraceList?.importBraceEntryList?.size ?: return "{ ... }"
            return "{ <$importCount items> }"
        }
        return "{ ... }"
    }

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean {
        if (node.psi is JaktImport) {
            val import = node.psi as JaktImport
            return import.importBraceList?.text?.contains('\n') ?: false
        }
        return false
    }

    private class FoldingVisitor(private val descriptors: MutableList<FoldingDescriptor>) : JaktVisitor() {
        override fun visitPsiElement(o: JaktPsiElement) {
        }

        override fun visitImport(o: JaktImport) {
            val importBraceList = o.importBraceList ?: return
            descriptors += FoldingDescriptor(o, importBraceList.textRange)
        }
    }
}
