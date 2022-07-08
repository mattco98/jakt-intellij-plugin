package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktForDecl
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.UnknownType

abstract class JaktForDeclMixin(node: ASTNode) : JaktNamedElement(node), JaktForDecl {
    override val jaktType: Type
        get() = UnknownType // TODO
}
