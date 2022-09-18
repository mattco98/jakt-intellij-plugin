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
            return findArgument(
                element.name ?: return null,
                exprType?.psiElement ?: return null,
            )
        }

        private fun findArgument(name: String, element: PsiElement): PsiElement? {
            return when (element) {
                is JaktFunction -> element.parameterList.parameterList.find {
                    it.name == name
                }
                is JaktStructDeclaration -> {
                    val field = element.structBody.structMemberList
                        .mapNotNull { it.structField }
                        .find { it.name == name }

                    if (field != null)
                        return field

                    val parent = element.superType?.type?.jaktType?.psiElement ?: return null
                    findArgument(name, parent)
                }
                is JaktEnumVariant -> element.normalEnumMemberBody?.structEnumMemberBodyPartList?.map {
                    it.structEnumMemberLabel
                }?.find {
                    it.identifier.text == name
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
