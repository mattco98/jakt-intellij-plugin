package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktExpression
import org.intellij.sdk.language.psi.JaktVariableDeclarationStatement
import org.intellij.sdk.language.psi.impl.JaktStatementImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.api.containingScope
import org.serenityos.jakt.plugin.psi.reference.JaktRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.TypeInference
import org.serenityos.jakt.utils.findChildOfType

abstract class JaktVariableDeclarationMixin(
    node: ASTNode,
) : JaktStatementImpl(node), JaktVariableDeclarationStatement {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val result = if (typeAnnotation != null) {
                typeAnnotation!!.jaktType
            } else {
                findChildOfType<JaktExpression>()?.let(TypeInference::inferType) ?: Type.Unknown
            }

            // TODO: Smarter caching
            CachedValueProvider.Result(result, this)
        }

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = Ref(this)

    class Ref(element: JaktVariableDeclarationStatement) : JaktRef<JaktVariableDeclarationStatement>(element) {
        override fun multiResolve(): List<PsiElement> {
            return element.containingScope?.findReferencesInOrBelow(element.name, getSubScopeParent(element)) ?: emptyList()
        }
    }
}
