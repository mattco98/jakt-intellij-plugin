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
    fun findDeclaration(name: String, resolutionStrategy: (JaktDeclaration) -> Boolean): JaktDeclaration? {
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

    fun resolveReference(name: String, resolutionStrategy: (JaktDeclaration) -> Boolean): PsiElement? {
        val decl = findDeclaration(name, resolutionStrategy)
        if (decl != null)
            return decl

        return JaktResolver(getPreviousScope() ?: return null).resolveReference(name, resolutionStrategy)
    }

    private fun getPreviousScope(): PsiElement? {
        if (scope is JaktFile)
            return null

        for ((current, parent) in scope.ancestorPairs(withSelf = true)) {
            if (parent is JaktScope)
                return parent

            if (parent is JaktBlock) {
                // Return the previous statement in the block, or the block itself if the
                // current element is the first child
                return current.prevNonWSSibling() ?: parent
            }
        }

        return null
    }

    fun getPreviousDeclarations(): Sequence<JaktDeclaration> = sequence {
        val scope = getPreviousScope()
        when (scope) {
            is JaktDeclaration -> yield(scope)
            is JaktScope -> yieldAll(scope.getDeclarations())
            null -> return@sequence
        }

        yieldAll(JaktResolver(scope!!).getPreviousDeclarations())
    }

    sealed interface ResolutionStrategy : (JaktDeclaration) -> Boolean

    private class AndResolutionStrategy(
        private val first: ResolutionStrategy,
        private val second: ResolutionStrategy,
    ) : ResolutionStrategy {
        override fun invoke(element: JaktDeclaration) = first(element) && second(element)
    }

    object STATIC : ResolutionStrategy {
        override fun invoke(element: JaktDeclaration) = when (element) {
            is JaktFunctionDeclaration -> (element.jaktType as Type.Function).thisParameter == null
            is JaktStructField -> false
            else -> true
        }
    }

    object INSTANCE : ResolutionStrategy {
        override fun invoke(element: JaktDeclaration) = when (element) {
            is JaktFunctionDeclaration -> (element.jaktType as Type.Function).thisParameter != null
            is JaktVariableDeclarationStatement, is JaktStructField -> true
            else -> false
        }
    }

    object TYPE : ResolutionStrategy {
        override fun invoke(element: JaktDeclaration) = (element as? JaktDeclaration)?.isTypeDeclaration == true
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
