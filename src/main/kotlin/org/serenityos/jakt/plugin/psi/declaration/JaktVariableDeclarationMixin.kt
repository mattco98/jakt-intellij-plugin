package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.toArray
import org.intellij.sdk.language.psi.JaktVariableDeclarationStatement
import org.intellij.sdk.language.psi.impl.JaktStatementImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.api.containingScope
import org.serenityos.jakt.plugin.psi.reference.JaktPsiReference
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.TypeInference

abstract class JaktVariableDeclarationMixin(
    node: ASTNode,
) : JaktStatementImpl(node), JaktDeclaration, JaktVariableDeclarationStatement {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val result = if (typeAnnotation != null) {
                typeAnnotation!!.jaktType
            } else {
                TypeInference.inferType(expression!!)
            }

            // TODO: Smarter caching
            CachedValueProvider.Result(result, this)
        }

    override fun getNameIdentifier() = plainQualifier.identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = JaktPsiReference.Decl(this)
}
