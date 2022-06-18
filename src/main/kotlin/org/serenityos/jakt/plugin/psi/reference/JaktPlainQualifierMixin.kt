package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.impl.JaktExpressionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolvePlainQualifier

abstract class JaktPlainQualifierMixin(node: ASTNode) : JaktExpressionImpl(node), JaktPlainQualifier {
    override val jaktType: Type
        get() = (reference.resolve() as? JaktTypeable)?.jaktType ?: Type.Unknown

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = singleRef(::resolvePlainQualifier)
}
