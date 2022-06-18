package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktParameter
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type

abstract class JaktParameterMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktParameter {
    override val jaktType: Type
        get() = typeAnnotation.jaktType
}
