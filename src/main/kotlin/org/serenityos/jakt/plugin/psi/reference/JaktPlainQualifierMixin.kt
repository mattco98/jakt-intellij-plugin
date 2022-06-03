package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.impl.JaktExpressionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.containingScope

abstract class JaktPlainQualifierMixin(node: ASTNode) : JaktExpressionImpl(node), JaktPlainQualifier {
    override fun getNameIdentifier(): PsiElement = identifier

    override fun getName(): String? = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = object : JaktPsiReference(this) {
        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            val decl = containingScope?.findDeclarationInOrAbove(element.name!!, from = element) ?: return emptyArray()
            return arrayOf(PsiElementResolveResult(decl))
        }
    }
}
