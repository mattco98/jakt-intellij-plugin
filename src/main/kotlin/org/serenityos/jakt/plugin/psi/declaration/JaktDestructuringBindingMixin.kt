package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktDestructuringBinding
import org.intellij.sdk.language.psi.JaktMatchCase
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.reference.multiRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveReferencesIn
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktDestructuringBindingMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktDestructuringBinding {
    override val jaktType: Type
        get() = Type.Unknown // TODO

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = multiRef {
        val matchBody = it.ancestorOfType<JaktMatchCase>()?.matchCaseTrail ?: return@multiRef emptyList()
        resolveReferencesIn(matchBody, name)
    }
}
