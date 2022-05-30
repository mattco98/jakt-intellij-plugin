package org.serenityos.jakt.plugin.psi.references

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.impl.JaktExpressionImpl
import org.serenityos.jakt.plugin.JaktReference
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.declarations.JaktDeclaration

abstract class JaktPlainQualifierImplMixin(
    node: ASTNode
) : JaktExpressionImpl(node), JaktPlainQualifier, JaktPsiReference {
    override var declaration: JaktDeclaration? = null

    override fun getReference() = JaktReference.Ident(this)

    override fun getNameIdentifier() = identifier

    override fun setName(name: String) = apply {
        identifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}
