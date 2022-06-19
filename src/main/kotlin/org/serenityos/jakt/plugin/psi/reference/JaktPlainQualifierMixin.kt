package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktExpression
import org.intellij.sdk.language.psi.JaktMatchExpression
import org.intellij.sdk.language.psi.JaktMatchPattern
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.api.jaktType
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolvePlainQualifier
import org.serenityos.jakt.utils.ancestorOfType
import org.serenityos.jakt.utils.findChildOfType

abstract class JaktPlainQualifierMixin(node: ASTNode) : JaktNamedElement(node), JaktPlainQualifier {
    override val jaktType: Type
        get() = (reference.resolve() as? JaktTypeable)?.jaktType ?: Type.Unknown

    override fun getReference() = singleRef { qualifier ->
        resolvePlainQualifier(qualifier) ?: run {
            // Attempt to resolve match enum shorthand
            // TODO: Should this happen somewhere else?

            if (qualifier.namespaceQualifierList.isNotEmpty() || qualifier.parent !is JaktMatchPattern)
                return@run null

            val matchExpression = qualifier.ancestorOfType<JaktMatchExpression>()!!
            val matchTarget = matchExpression.findChildOfType<JaktExpression>()!!
            val matchType = matchTarget.jaktType

            if (matchType is Type.Enum) {
                val decl = matchType.declaration as? JaktPsiScope ?: return@run null
                decl.getDeclarations().find { it.name == qualifier.name }
            } else null
        }
    }
}
