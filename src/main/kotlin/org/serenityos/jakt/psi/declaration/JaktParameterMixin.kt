package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.api.JaktParameter
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type

abstract class JaktParameterMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktParameter {
    override val jaktType: Type
        get() = typeAnnotation.jaktType
}
