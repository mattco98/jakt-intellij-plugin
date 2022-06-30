package org.serenityos.jakt.type

import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.ancestorPairs
import org.serenityos.jakt.psi.api.JaktScope
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.declaration.*
import org.serenityos.jakt.psi.prevNonWSSibling
import org.serenityos.jakt.psi.reference.isType

class JaktResolver(private val scope: PsiElement) {
    fun findDeclaration(name: String, resolutionStrategy: (PsiElement) -> Boolean): JaktDeclaration? {
        return when (scope) {
            is JaktVariableDeclarationStatement -> {
                // TODO: Resolve to identifier
                scope.takeIf { it.name == name && resolutionStrategy(it) }
            }
            is JaktScope -> scope.getDeclarations().find { it.name == name && resolutionStrategy(it) }
            is JaktGeneric -> scope.getDeclGenericBounds().find { it.name == name && resolutionStrategy(it) }
            else -> null
        }?.unwrapImport()
    }

    fun resolveReference(name: String, resolutionStrategy: (PsiElement) -> Boolean): PsiElement? {
        val decl = findDeclaration(name, resolutionStrategy)
        if (decl != null)
            return decl

        return JaktResolver(getPreviousScope() ?: return null).resolveReference(name, resolutionStrategy)
    }

    private fun getPreviousScope(): PsiElement? {
        if (scope is JaktFile)
            return null

        for ((current, parent) in scope.ancestorPairs(withSelf = true)) {
            if (parent is JaktFunctionDeclaration)
                return parent

            if (parent is JaktBlock) {
                // Return the previous statement in the block, or the block itself if the
                // current element is the first child
                return current.prevNonWSSibling() ?: parent
            }

            if (current is JaktDeclaration) {
                // We are not in a block, so current must be a function/struct/enum/namespace.
                // Return the parent
                return parent
            }
        }

        return null
    }

    sealed interface ResolutionStrategy : (PsiElement) -> Boolean

    private class AndResolutionStrategy(
        private val first: ResolutionStrategy,
        private val second: ResolutionStrategy,
    ) : ResolutionStrategy {
        override fun invoke(element: PsiElement) = first(element) && second(element)
    }

    object STATIC : ResolutionStrategy {
        override fun invoke(element: PsiElement) = when (element) {
            is JaktStructDeclaration, is JaktEnumDeclaration, is JaktNamespaceDeclaration -> true
            is JaktFunctionDeclaration -> (element.jaktType as Type.Function).thisParameter == null
            is JaktEnumVariant -> true
            is JaktImportStatement, is JaktImportBraceEntry -> true
            else -> false
        }
    }

    object INSTANCE : ResolutionStrategy {
        override fun invoke(element: PsiElement) = when (element) {
            is JaktStructField -> true
            is JaktFunctionDeclaration -> (element.jaktType as Type.Function).thisParameter != null
            else -> false
        }
    }

    object TYPE : ResolutionStrategy {
        override fun invoke(element: PsiElement) = (element as? JaktDeclaration)?.isTypeDeclaration == true
    }

    companion object {
        fun resolveQualifier(qualifier: JaktPlainQualifier): PsiElement? {
            if (qualifier.isType)
                return resolveType(qualifier)
            return resolveQualifierHelper(qualifier, STATIC)
        }

        private fun resolveType(qualifier: JaktPlainQualifier): PsiElement? {
            // Check for shorthand enum type in `is` expression
            if (qualifier.plainQualifier == null) {
                val unaryExpr = qualifier.ancestorOfType<JaktUnaryExpression>()
                if (unaryExpr?.keywordIs != null && (unaryExpr.type as JaktPlainType).plainQualifier == qualifier) {
                    val baseType = unaryExpr.expression.jaktType
                    if (baseType is Type.EnumVariant && baseType.name == qualifier.name)
                        return baseType.declaration
                }
            }

            return resolveQualifierHelper(qualifier, AndResolutionStrategy(STATIC, TYPE))
        }

        private fun resolveQualifierHelper(
            qualifier: JaktPlainQualifier,
            resolutionStrategy: ResolutionStrategy,
        ): PsiElement? {
            val name = qualifier.name ?: return null
            val prev = qualifier.plainQualifier?.let { resolveQualifierHelper(it, resolutionStrategy) }
                ?: return JaktResolver(qualifier).resolveReference(name, resolutionStrategy)
            return JaktResolver(prev).findDeclaration(name, resolutionStrategy)
        }

        private fun JaktDeclaration.unwrapImport(): JaktDeclaration? = when (this) {
            is JaktImportStatement -> resolveFile()
            is JaktImportBraceEntry -> resolveElement()
            else -> this
        }
    }
}
