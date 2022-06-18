package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.JaktThisExpression
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.named.JaktNameIdentifierOwner
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.utils.ancestorsOfType

// TODO: It feels weird to inherit from JaktNamedElement here,
//       but JaktRef requires it
abstract class JaktThisExpressionMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktThisExpression, JaktNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement = thisKeyword

    override fun setName(name: String): PsiElement = throw IncorrectOperationException()

    override fun getReference() = singleRef {
        for (decl in it.ancestorsOfType<JaktPsiScope>()) {
            if (decl is JaktStructDeclaration || decl is JaktEnumDeclaration)
                return@singleRef decl
        }

        null
    }
}
