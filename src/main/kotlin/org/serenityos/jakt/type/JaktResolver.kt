package org.serenityos.jakt.type

import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.ancestorPairs
import org.serenityos.jakt.psi.api.JaktScope
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.declaration.*
import org.serenityos.jakt.psi.prevNonWSSibling
import org.serenityos.jakt.psi.reference.isType
import org.serenityos.jakt.utils.unreachable

class JaktResolver(private val scope: PsiElement) {
    fun findDeclaration(name: String, resolutionStrategy: (JaktDeclaration) -> Boolean): JaktDeclaration? {
        if (scope is JaktGeneric) {
            scope.getDeclGenericBounds()
                .find { it.name == name && it.unwrapImport()?.let(resolutionStrategy) == true }
                ?.let { return it.unwrapImport() }
        }

        if (scope is JaktScope) {
            scope.getDeclarations()
                .find { it.name == name && it.unwrapImport()?.let(resolutionStrategy) == true }
                ?.let { return it.unwrapImport() }
        }

        // Look for bindings in guard and if statements
        if (scope is JaktGuardStatement || scope is JaktIfStatement) {
            val expression = when (scope) {
                is JaktGuardStatement -> scope.expression
                is JaktIfStatement -> scope.expression
                else -> unreachable()
            }

            val isExpressions = collectLongLivingIsExpressions(expression)
            isExpressions.asReversed().forEach { expr ->
                expr.matchPattern.destructuringPartList.forEach {
                    val binding = it.destructuringBinding
                    if (binding.text == name && resolutionStrategy(binding))
                        return binding
                }
            }
        }

        return null
    }

    private fun collectLongLivingIsExpressions(
        element: JaktExpression,
    ): List<JaktIsExpression> {
        val list = mutableListOf<JaktIsExpression>()

        fun collect(expr: JaktExpression) {
            when (expr) {
                is JaktLogicalAndBinaryExpression -> {
                    collect(expr.left)
                    expr.right?.also { collect(it) }
                }
                is JaktIsExpression -> list.add(expr)
            }
        }

        collect(element)
        return list
    }

    fun resolveReference(name: String, resolutionStrategy: (JaktDeclaration) -> Boolean): PsiElement? {
        val decl = findDeclaration(name, resolutionStrategy)
        if (decl != null)
            return decl

        val previousScope = getPreviousScope()
            ?: return scope.jaktProject.findPreludeDeclaration(name)?.takeIf(resolutionStrategy)

        return JaktResolver(previousScope).resolveReference(name, resolutionStrategy)
    }

    private fun getPreviousScope(): PsiElement? {
        if (scope is JaktFile)
            return null

        for ((current, parent) in scope.ancestorPairs(withSelf = true)) {
            if (parent is JaktVariableDeclarationStatement) {
                // The variable is in the RHS expression of a variable declaration statement,
                // so we don't want to count this declaration
                continue
            }

            if (parent is JaktScope)
                return parent

            if (parent is JaktBlock) {
                // Return the previous statement in the block, or the block itself if the
                // current element is the first child
                return current.prevNonWSSibling()?.let {
                    // We cannot consider the previous statement in the block if that
                    // statement is an if statement, since it's is-expression bindings
                    // are only available inside it
                    if (it is JaktIfStatement) null else it
                } ?: parent
            }

            // If there is an IfStatement in the parent chain, check it for is-expression
            // bindings. Note that we don't check for JaktGuardStatement, as those bindings
            // are available _after_ the guard statement, not inside of it.
            if (parent is JaktIfStatement)
                return parent
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
            is JaktFunction -> !(element.jaktType as FunctionType).hasThis
            is JaktStructField -> false
            else -> true
        }
    }

    object INSTANCE : ResolutionStrategy {
        override fun invoke(element: JaktDeclaration) = when (element) {
            is JaktFunction -> (element.jaktType as FunctionType).hasThis
            is JaktVariableDecl, is JaktStructField -> true
            else -> false
        }
    }

    object TYPE : ResolutionStrategy {
        override fun invoke(element: JaktDeclaration) = (element as? JaktDeclaration)?.isTypeDeclaration == true
    }

    companion object {
        fun resolveQualifier(qualifier: JaktPlainQualifier): PsiElement? {
            if (qualifier.isType)
                return resolveQualifierHelper(qualifier, AndResolutionStrategy(STATIC, TYPE))

            // Check for shorthand enum type in match-expression and is-expression patterns
            if (qualifier.plainQualifier == null) {
                var matchPattern = qualifier.ancestorOfType<JaktMatchPattern>()
                if (matchPattern?.plainQualifierExpression?.plainQualifier == qualifier) {
                    val matchTarget = matchPattern.ancestorOfType<JaktMatchExpression>()?.expression?.jaktType
                    if (matchTarget is EnumType)
                        matchTarget.variants[qualifier.name!!]?.let { return it.psiElement }
                }

                val isExpr = qualifier.ancestorOfType<JaktIsExpression>()
                matchPattern = isExpr?.matchPattern
                if (matchPattern?.plainQualifierExpression?.plainQualifier == qualifier) {
                    val baseType = isExpr!!.expression.jaktType
                    if (baseType is EnumVariantType && baseType.name == qualifier.name)
                        return baseType.psiElement
                }
            }

            return resolveQualifierHelper(qualifier, STATIC)
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
