package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.JaktTypeable
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktPlainQualifier
import org.serenityos.jakt.psi.api.JaktPlainQualifierExpression
import org.serenityos.jakt.psi.api.JaktPlainType
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.JaktResolver
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.UnknownType

abstract class JaktPlainQualifierMixin(node: ASTNode) : JaktNamedElement(node), JaktPlainQualifier {
    override val jaktType: Type
        get() = (reference.resolve() as? JaktTypeable)?.jaktType ?: UnknownType

    override fun getReference() = singleRef(JaktResolver::resolveQualifier)
}

val JaktPlainQualifier.isBase: Boolean
    get() = findChildrenOfType<JaktPlainQualifier>().isEmpty()

val JaktPlainQualifier.index: Int
    get() = plainQualifier?.index?.plus(1) ?: 0

val JaktPlainQualifier.hasNamespace: Boolean
    get() = plainQualifier != null

val JaktPlainQualifier.exprAncestor: JaktPlainQualifierExpression?
    get() = ancestorOfType<JaktPlainQualifierExpression>()

val JaktPlainQualifier.isType: Boolean
    get() = parent is JaktPlainType
