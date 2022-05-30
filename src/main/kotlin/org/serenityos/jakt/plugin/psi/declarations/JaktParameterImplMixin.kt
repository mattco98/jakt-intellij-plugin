package org.serenityos.jakt.plugin.psi.declarations

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.intellij.sdk.language.psi.JaktParameter
import org.serenityos.jakt.plugin.JaktReference
import org.serenityos.jakt.plugin.psi.JaktNamedElement
import org.serenityos.jakt.plugin.psi.references.JaktPsiReference

abstract class JaktParameterImplMixin(
    node: ASTNode
) : JaktNamedElement(node), JaktDeclaration, JaktParameter {
    var owningFunction: JaktFunctionDeclaration? = null
    override var declarationReferences: MutableList<JaktPsiReference>? = null

    override fun getReference() = JaktReference.Decl(this)

    override fun getIdentifyingRange() = identifier.textRangeInParent
}
