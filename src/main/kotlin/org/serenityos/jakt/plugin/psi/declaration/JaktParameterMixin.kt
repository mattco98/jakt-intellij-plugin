package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.intellij.sdk.language.psi.JaktParameter
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.reference.JaktRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveDeclarationIn
import org.serenityos.jakt.plugin.type.resolveReferencesIn
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktParameterMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktParameter {
    override val jaktType: Type
        get() = typeAnnotation.jaktType

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = Ref(this)

    class Ref(element: JaktParameter) : JaktRef<JaktParameter>(element) {
        override fun multiResolve(): List<PsiElement> {
            return resolveReferencesIn(element.ancestorOfType<JaktPsiScope>() ?: return emptyList(), element.name)
        }
    }
}
