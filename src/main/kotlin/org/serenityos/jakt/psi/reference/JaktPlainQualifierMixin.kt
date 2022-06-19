package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.resolvePlainQualifier

abstract class JaktPlainQualifierMixin(node: ASTNode) : JaktNamedElement(node), JaktPlainQualifier {
    override val jaktType: Type
        get() = (reference.resolve() as? JaktTypeable)?.jaktType ?: Type.Unknown

    override fun getReference() = singleRef(::resolvePlainQualifier)
}
