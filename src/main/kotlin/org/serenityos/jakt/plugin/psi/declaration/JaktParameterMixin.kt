package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktParameter
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.psi.reference.multiRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveReferencesIn
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktParameterMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktParameter {
    override val jaktType: Type
        get() = typeAnnotation.jaktType

    override fun getReference() = multiRef {
        val owner = it.ancestorOfType<JaktPsiScope>() ?: return@multiRef emptyList()
        resolveReferencesIn(owner, it.name)
    }
}
