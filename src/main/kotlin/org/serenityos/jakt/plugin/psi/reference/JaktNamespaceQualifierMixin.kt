package org.serenityos.jakt.plugin.psi.reference

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktNamespaceQualifier
import org.intellij.sdk.language.psi.JaktTopLevelDefinition
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.api.containingScope
import org.serenityos.jakt.plugin.psi.api.findDeclarationInOrAbove
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktNamespaceQualifierMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktNamespaceQualifier {
    override val jaktType: Type
        get() {
            val namespace = prevSibling?.reference?.resolve()?.let {
                check(it is JaktTypeable)
                val type = it.jaktType
                check(type is Type.Namespace)
                type
            }

            return if (namespace != null) {
                namespace.members.firstOrNull { it.name == name }
            } else {
                containingScope?.findDeclarationInOrAbove(name)?.jaktType
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
            if (element.prevSibling == null)
                return listOfNotNull(resolvePlainQualifier(element))

            val prev = element.prevSibling.reference?.resolve() ?: return emptyList()
            return listOfNotNull(prev.findChildrenOfType<JaktTopLevelDefinition>().firstOrNull {
                it.name == element.name
            })
        }
    }
}
