package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktGenericSpecialization
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.JaktPlainQualifierExpression
import org.intellij.sdk.language.psi.JaktPlainType
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.findChildOfType
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
    get() = (parent as? JaktPlainQualifierMixin)?.index?.plus(1) ?: 0

val JaktPlainQualifier.hasNamespace: Boolean
    get() = plainQualifier != null

val JaktPlainQualifier.exprAncestor: JaktPlainQualifierExpression?
    get() = ancestorOfType<JaktPlainQualifierExpression>()

val JaktPlainQualifier.isType: Boolean
    get() {
        var rootQualifier = this
        repeat(rootQualifier.index) {
            rootQualifier = rootQualifier.parent as JaktPlainQualifier
        }
        return rootQualifier.parent is JaktPlainType
    }
