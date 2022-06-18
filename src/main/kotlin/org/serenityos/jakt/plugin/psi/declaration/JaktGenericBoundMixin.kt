package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktGenericBound
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.reference.JaktRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveReferencesIn
import org.serenityos.jakt.utils.ancestorOfType

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
            return resolveReferencesIn(element.ancestorOfType<JaktGeneric>() ?: return emptyList(), element.name)
        }
    }
}
