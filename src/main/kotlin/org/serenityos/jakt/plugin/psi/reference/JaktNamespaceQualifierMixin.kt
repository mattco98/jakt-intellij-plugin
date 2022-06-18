package org.serenityos.jakt.plugin.psi.reference

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktNamespaceQualifier
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveDeclarationAbove
import org.serenityos.jakt.plugin.type.resolveDeclarationIn

abstract class JaktNamespaceQualifierMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktNamespaceQualifier {
    override val jaktType: Type
        get() {
            val namespace = prevSibling?.reference?.resolve()?.let {
                (it as? JaktTypeable)?.jaktType as? Type.Namespace
            }

            return if (namespace != null) {
                namespace.members.firstOrNull { it.name == name }
            } else {
                resolveDeclarationAbove(this, name)?.jaktType
            } ?: Type.Unknown
        }

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = Ref(this)

    class Ref(element: JaktNamespaceQualifier) : JaktRef<JaktNamespaceQualifier>(element) {
        override fun multiResolve(): List<PsiElement> {
            return if (element.prevSibling == null) {
                resolveDeclarationAbove(element)
            } else {
                resolveDeclarationIn(
                    element.prevSibling.reference?.resolve() ?: return emptyList(),
                    element.name!!,
                )
            }.let(::listOfNotNull)
        }
    }
}
