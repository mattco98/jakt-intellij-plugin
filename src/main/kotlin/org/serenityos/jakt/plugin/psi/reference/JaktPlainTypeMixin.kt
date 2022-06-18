package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktPlainType
import org.intellij.sdk.language.psi.impl.JaktTypeImpl
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.type.resolveDeclarationAbove
import org.serenityos.jakt.utils.ancestorPairsOfType
import org.serenityos.jakt.utils.ancestorsOfType
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktPlainTypeMixin(node: ASTNode) : JaktTypeImpl(node), JaktPlainType {
    override fun getNameIdentifier(): PsiElement = findChildrenOfType(JaktTypes.IDENTIFIER).last()

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = Ref(this)

    class Ref(element: JaktPlainType) : JaktRef<JaktPlainType>(element) {
        override fun multiResolve(): List<PsiElement> {
            return listOfNotNull(resolveDeclarationAbove(element))
        }
    }
}
