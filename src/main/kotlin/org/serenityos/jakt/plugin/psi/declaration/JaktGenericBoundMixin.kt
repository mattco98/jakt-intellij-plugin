package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktGenericBound
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.containingScope
import org.serenityos.jakt.plugin.psi.reference.JaktRef
import org.serenityos.jakt.plugin.type.Type

abstract class JaktGenericBoundMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktGenericBound {
    override val jaktType: Type
        get() = Type.TypeVar(name)

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = Ref(this)

    class Ref(element: JaktGenericBound) : JaktRef<JaktGenericBound>(element) {
        override fun multiResolve(): List<PsiElement> {
            return element.containingScope?.findReferencesInOrBelow(element.name!!, getSubScopeParent(element)) ?: emptyList()
        }
    }
}
