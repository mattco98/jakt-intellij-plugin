package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.*
import org.serenityos.jakt.psi.jaktType
import org.serenityos.jakt.psi.named.JaktNamedElement

abstract class JaktArgumentMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktArgument {
    override fun getReference() = object : JaktRef<JaktArgument>(this) {
        override fun singleResolve(): PsiElement? {
            // Unlabled arguments have no references
            if (identifier == null)
                return null

            val exprType = element.ancestorOfType<JaktCallExpression>()?.expression?.jaktType
            return when (val callTarget = exprType?.psiElement) {
                is JaktFunction -> callTarget.parameterList.parameterList.find {
                    it.name == element.name
                }
                is JaktStructDeclaration -> callTarget.structBody.structMemberList
                    .mapNotNull { it.structField }
                    .find { it.name == element.name }
                is JaktEnumVariant -> callTarget.normalEnumMemberBody?.structEnumMemberBodyPartList?.map {
                    it.structEnumMemberLabel
                }?.find {
                    it.identifier.text == element.name
                }
                else -> null
            }
        }

        override fun isReferenceTo(element: PsiElement): Boolean {
            // We usually want to return false here, since we don't really want to resolve struct
            // fields and function parameters to their usages in a call expression. Both of those
            // elements have more important usages targets; function parameters should show usages
            // in the function they belong to, and struct fields should show field access expressions.
            // However, named enum fields don't have a more important usage target, so we allow
            // those to resolve to their usages in ctor calls.
            return ancestorOfType<JaktCallExpression>()?.expression?.jaktType?.psiElement is JaktEnumVariant
        }
    }
}

val JaktArgument.isLabeled: Boolean
    get() = identifier != null
