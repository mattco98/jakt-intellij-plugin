package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.JaktPlainQualifierExpr
import org.intellij.sdk.language.psi.JaktPlainType
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.JaktResolver
import org.serenityos.jakt.type.Type

abstract class JaktPlainQualifierExprMixin(node: ASTNode) : JaktNamedElement(node), JaktPlainQualifierExpr {
    override val jaktType: Type
        get() = plainQualifier.jaktType

    override fun getReference() = plainQualifier.reference
}
