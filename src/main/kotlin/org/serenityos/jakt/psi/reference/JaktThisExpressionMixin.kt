package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.serenityos.jakt.psi.JaktScope
import org.serenityos.jakt.psi.ancestorsOfType
import org.serenityos.jakt.psi.api.JaktEnumDeclaration
import org.serenityos.jakt.psi.api.JaktStructDeclaration
import org.serenityos.jakt.psi.api.JaktThisExpression
import org.serenityos.jakt.psi.named.JaktNameIdentifierOwner
import org.serenityos.jakt.psi.named.JaktNamedElement

// TODO: It feels weird to inherit from JaktNamedElement here,
//       but JaktRef requires it
abstract class JaktThisExpressionMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktThisExpression, JaktNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement = thisKeyword

    override fun setName(name: String): PsiElement = throw IncorrectOperationException()

    override fun getReference() = singleRef {
        for (decl in it.ancestorsOfType<JaktScope>()) {
            if (decl is JaktStructDeclaration || decl is JaktEnumDeclaration)
                return@singleRef decl
        }

        null
    }
}
