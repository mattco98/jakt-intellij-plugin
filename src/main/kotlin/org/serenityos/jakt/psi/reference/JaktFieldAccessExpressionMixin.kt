package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.ancestorsOfType
import org.serenityos.jakt.psi.api.JaktFieldAccessExpression
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.api.JaktStructDeclaration
import org.serenityos.jakt.psi.api.JaktStructMethod
import org.serenityos.jakt.psi.findChildOfType
import org.serenityos.jakt.psi.named.JaktNamedElement

abstract class JaktFieldAccessExpressionMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktFieldAccessExpression {
    override fun getReference() = singleRef {
        for (decl in it.ancestorsOfType<JaktStructDeclaration>()) {
            decl.structBody.structMemberList.forEach { member ->
                if (member.structField?.identifier?.text == it.name)
                    return@singleRef member.structField
                if (member.structMethod?.function?.identifier?.text == it.name)
                    return@singleRef member.structMethod?.function
            }
        }

        null
    }
}

val JaktStructMethod.function: JaktFunction?
    get() = findChildOfType()
