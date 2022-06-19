package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktFieldAccessExpression
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.utils.ancestorsOfType

abstract class JaktFieldAccessExpressionMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktFieldAccessExpression {
    override fun getReference() = singleRef {
        for (decl in it.ancestorsOfType<JaktStructDeclaration>()) {
            decl.structBody.structMemberList.forEach { member ->
                if (member.structField?.identifier?.text == it.name)
                    return@singleRef member.structField
                if (member.functionDeclaration?.identifier?.text == it.name)
                    return@singleRef member.functionDeclaration
            }
        }

        null
    }
}
