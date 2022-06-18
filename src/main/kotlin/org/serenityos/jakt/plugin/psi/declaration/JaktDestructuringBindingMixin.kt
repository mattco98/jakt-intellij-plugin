package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktDestructuringBinding
import org.intellij.sdk.language.psi.JaktMatchCase
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.psi.reference.multiRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveReferencesIn
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktDestructuringBindingMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktDestructuringBinding {
    override val jaktType: Type
        get() = Type.Unknown // TODO

    override fun getReference() = multiRef {
        val matchBody = it.ancestorOfType<JaktMatchCase>()?.matchCaseTrail ?: return@multiRef emptyList()
        resolveReferencesIn(matchBody, name)
    }
}
