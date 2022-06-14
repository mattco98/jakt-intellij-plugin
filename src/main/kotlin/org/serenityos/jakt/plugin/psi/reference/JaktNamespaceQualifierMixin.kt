package org.serenityos.jakt.plugin.psi.reference

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
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
) : ASTWrapperPsiElement(node), JaktNamespaceQualifier, JaktTypeable {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val namespace = prevSibling?.reference?.resolve()?.let {
                check(it is JaktTypeable)
                val type = it.jaktType
                check(type is Type.Namespace)
                type
            }

            val result = if (namespace != null) {
                namespace.members.firstOrNull { it.name == name }
            } else {
                containingScope?.findDeclarationInOrAbove(name)?.jaktType
            } ?: Type.Unknown

            CachedValueProvider.Result(result, this)
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
