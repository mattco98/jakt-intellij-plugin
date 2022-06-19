package org.serenityos.jakt.type

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.api.JaktPsiScope
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import org.serenityos.jakt.psi.declaration.JaktGeneric
import org.serenityos.jakt.psi.declaration.JaktImportBraceEntryMixin
import org.serenityos.jakt.psi.declaration.JaktImportStatementMixin
import org.serenityos.jakt.utils.ancestorOfType
import org.serenityos.jakt.utils.ancestorsOfType
import org.serenityos.jakt.utils.findChildOfType

fun JaktDeclaration.unwrapImport(): JaktDeclaration? = when (this) {
    is JaktImportStatementMixin -> resolveFile()
    is JaktImportBraceEntryMixin -> resolveElement()
    else -> this
}

fun resolveDeclarationIn(scope: PsiElement, name: String): JaktDeclaration? {
    return when (scope) {
        is JaktPsiScope -> scope.getDeclarations().find { it.name == name }?.unwrapImport()
        is JaktGeneric -> scope.getDeclGenericBounds().find { it.name == name }
        else -> null
    }
}

fun resolveDeclarationIn(type: Type, name: String): Type {
    return when (type) {
        is Type.Namespace -> type.members.find { it.name == name } ?: Type.Unknown
        is Type.Parameterized -> resolveDeclarationIn(type.underlyingType, name)
        is Type.Struct -> type.fields.entries.find { it.key == name }?.value
            ?: type.methods.entries.find { it.key == name }?.value
            ?: Type.Unknown
        is Type.Enum -> type.methods.entries.find { it.key == name }?.value ?: Type.Unknown
        else -> Type.Unknown
    }
}

fun resolveDeclarationAbove(scope: PsiNameIdentifierOwner): JaktDeclaration? =
    scope.name?.let { resolveDeclarationAbove(scope, it) }

fun resolveDeclarationAbove(scope: PsiElement, name: String): JaktDeclaration? {
    resolveDeclarationIn(scope, name)?.let { return it }

    for (parent in scope.ancestorsOfType<JaktPsiScope>())
        resolveDeclarationIn(parent, name)?.let { return it }

    return scope.jaktProject.findPreludeType(name)
}

private fun resolveEnumShorthand(type: Type, name: String): JaktDeclaration? {
    val scope = (type as? Type.Enum)?.declaration as? JaktPsiScope
    return scope?.getDeclarations()?.find { it.name == name }
}

fun resolvePlainQualifier(qualifier: JaktPlainQualifier): JaktDeclaration? {
    return if (qualifier.namespaceQualifierList.isNotEmpty()) {
        val nsRef = qualifier.namespaceQualifierList.last().reference?.resolve() ?: return null
        resolveDeclarationIn(nsRef, qualifier.name!!)
    } else {
        resolveDeclarationAbove(qualifier) ?: run {
            // Try to resolve match enum shorthand
            val matchParent = qualifier.parent as? JaktMatchPattern ?: return@run null
            val matchExpression = matchParent.ancestorOfType<JaktMatchExpression>()!!
            val matchTarget = matchExpression.findChildOfType<JaktExpression>()!!
            resolveEnumShorthand(matchTarget.jaktType, qualifier.name!!)
        }
    }
}

// TODO: This is a bit of a hack to resolve constructor functions, since otherwise the return
//       type would resolve to the same function
private fun resolveTypeDeclarationAbove(scope: PsiElement, name: String): JaktDeclaration? {
    var decl = resolveDeclarationAbove(scope, name)
    while (decl is JaktFunctionDeclaration || decl is JaktExternStructMethod || decl is JaktExternFunctionDeclaration)
        decl = resolveDeclarationAbove(decl, name)
    return decl
}

fun resolvePlainType(plainType: JaktPlainType): JaktDeclaration? {
    return if (plainType.namespaceQualifierList.isNotEmpty()) {
        val nsRef = plainType.namespaceQualifierList.last().reference?.resolve() ?: return null
        resolveDeclarationIn(nsRef, plainType.name!!)
    } else {
        resolveTypeDeclarationAbove(plainType, plainType.name!!) ?: run {
            // Try to resolve is enum shorthand
            val postfixParent = plainType.parent as? JaktPostfixUnaryExpression ?: return@run null
            val exprType = postfixParent.expression.takeIf { postfixParent.keywordIs != null }?.jaktType
                ?: return@run null
            resolveEnumShorthand(exprType, plainType.name!!)
        }
    }
}

fun resolveAccess(access: JaktAccess): JaktDeclaration? {
    val accessExpr = access.ancestorOfType<JaktAccessExpression>()!!
    val baseType = TypeInference.inferType(accessExpr.expression).resolveToBuiltinType(access.project)
    val baseDecl = (baseType as? Type.TopLevelDecl)?.declaration ?: return null
    return resolveDeclarationIn(baseDecl, access.name!!)
}
