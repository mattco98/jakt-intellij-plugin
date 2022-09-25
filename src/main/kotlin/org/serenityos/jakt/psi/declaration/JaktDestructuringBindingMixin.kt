package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.*
import org.serenityos.jakt.psi.caching.typeCache
import org.serenityos.jakt.psi.jaktType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.*

abstract class JaktDestructuringBindingMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktDestructuringBinding {
    override val jaktType: Type
        get() = typeCache().resolveWithCaching(this) {
            getTypeOfElsePattern()?.let { return@resolveWithCaching it }

            val part = parent as? JaktDestructuringPart ?: return@resolveWithCaching UnknownType
            val pattern = part.parent as? JaktMatchPattern ?: return@resolveWithCaching UnknownType

            val qualifier = pattern.plainQualifierExpression.plainQualifier
            val enumVariant = qualifier.jaktType as? EnumVariantType ?: return@resolveWithCaching UnknownType

            if (enumVariant.isStructLike) {
                val label = part.destructuringLabel?.name ?: identifier.text
                enumVariant.members.find { it.first == label }
            } else {
                val index = pattern.destructuringPartList.indexOfFirst { it.destructuringBinding == this }
                enumVariant.members.getOrNull(index)
            }?.second ?: UnknownType
        }

    private fun getTypeOfElsePattern(): Type? {
        val elseHead = ancestorOfType<JaktMatchCaseElseHead>() ?: return null
        val label = ancestorOfType<JaktDestructuringPart>()?.destructuringLabel?.identifier?.text ?: name

        val index = elseHead.destructuringPartList.indexOfFirst { it.destructuringBinding == this }
        if (index == -1)
            return UnknownType

        val match = elseHead.ancestorOfType<JaktMatchExpression>()
        val enumType = match?.expression?.jaktType?.let {
            when (it) {
                is EnumVariantType -> it.parentType as? EnumType
                is EnumType -> it
                else -> null
            }
        } ?: return UnknownType

        var resultType: Type? = null
        val variantsToExclude = match.matchBody?.matchCaseList?.flatMap { case ->
            case.matchCaseHead.matchPatternList.mapNotNull { pattern ->
                (pattern.plainQualifierExpression.jaktType as? EnumVariantType)?.name
            } + case.matchCaseHead.expressionList.mapNotNull { expr ->
                (expr.reference?.resolve() as? JaktEnumVariant)?.name
            }
        }?.toSet().orEmpty()

        for (variantType in enumType.variants.values) {
            if (variantType.name in variantsToExclude)
                continue

            // All remaining enum variant must match a pattern in order to compile
            if (!elsePatternMatchesVariant(variantType, elseHead))
                return UnknownType

            val currentType = if (variantType.isStructLike) {
                variantType.members.find { it.first == label }?.second ?: return UnknownType
            } else variantType.members[index].second

            if (resultType != null && !resultType.equivalentTo(currentType)) {
                // Conflicting types for enum members, should be an error
                return UnknownType
            }

            resultType = currentType
        }

        return resultType
    }

    private fun elsePatternMatchesVariant(type: EnumVariantType, elseHead: JaktMatchCaseElseHead): Boolean {
        if (type.value != null)
            return false

        if (!type.isStructLike)
            return elseHead.destructuringPartList.size == type.members.size

        val labels = elseHead.destructuringPartList.map {
            it.destructuringLabel?.identifier ?: it.destructuringBinding.identifier
        }.map {
            it.text
        }

        for (label in labels) {
            if (type.members.none { it.first == label })
                return false
        }

        return true
    }
}
