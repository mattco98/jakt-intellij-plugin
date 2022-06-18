package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktExpression
import org.intellij.sdk.language.psi.JaktVariableDeclarationStatement
import org.intellij.sdk.language.psi.impl.JaktStatementImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.reference.multiRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.TypeInference
import org.serenityos.jakt.plugin.type.resolveReferencesIn
import org.serenityos.jakt.utils.ancestorOfType
import org.serenityos.jakt.utils.findChildOfType

abstract class JaktVariableDeclarationMixin(
    node: ASTNode,
) : JaktStatementImpl(node), JaktVariableDeclarationStatement {
    override val jaktType: Type
        get() = if (typeAnnotation != null) {
            typeAnnotation!!.jaktType
        } else {
            findChildOfType<JaktExpression>()?.let(TypeInference::inferType) ?: Type.Unknown
        }

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getTextOffset(): Int = nameIdentifier.textOffset

    override fun getReference() = multiRef {
        val owner = it.ancestorOfType<JaktPsiScope>() ?: return@multiRef emptyList()
        resolveReferencesIn(owner, it.name)
    }
}
