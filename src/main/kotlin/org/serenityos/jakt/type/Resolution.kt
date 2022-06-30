package org.serenityos.jakt.type

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktPsiScope
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.declaration.*
import org.serenityos.jakt.psi.findChildOfType
import org.serenityos.jakt.psi.reference.hasNamespace

fun JaktDeclaration.unwrapImport(): JaktDeclaration? = when (this) {
    is JaktImportStatement -> resolveFile()
    is JaktImportBraceEntry -> resolveElement()
    else -> this
}

//////////////////
// DECLARATIONS //
//////////////////

fun resolveDeclarationIn(element: PsiElement, name: String): JaktDeclaration? {
    return when (element) {
        is JaktPsiScope -> element.getDeclarations().find { it.name == name }?.unwrapImport()
        is JaktGeneric -> element.getDeclGenericBounds().find { it.name == name }
        else -> null
    }
}

fun resolveDeclarationIn(type_: Type, name: String): Type {
    return when (val type = type_.unwrap()) {
        is Type.Namespace -> type.members.find { it.name == name } ?: Type.Unknown
        is Type.Struct -> type.fields.entries.find { it.key == name }?.value
            ?: type.methods.entries.find { it.key == name }?.value
            ?: Type.Unknown
        is Type.Enum -> type.methods.entries.find { it.key == name }?.value ?: Type.Unknown
        else -> Type.Unknown
    }
}

fun resolveDeclarationAbove(element: PsiNameIdentifierOwner): JaktDeclaration? =
    element.name?.let { resolveDeclarationAbove(element, it) }

fun resolveDeclarationAbove(element: PsiElement, name: String): JaktDeclaration? {
    resolveDeclarationIn(element, name)?.let { return it }
    val parent = element.ancestorOfType<JaktPsiScope>() ?: return element.jaktProject.findPreludeDeclaration(name)
    return resolveDeclarationAbove(parent, name)
}

private fun resolveEnumShorthand(type: Type, name: String): JaktDeclaration? {
    val scope = (type as? Type.Enum)?.declaration as? JaktPsiScope
    return scope?.getDeclarations()?.find { it.name == name }
}

fun resolvePlainQualifier(element: JaktPlainQualifier): JaktDeclaration? {
    if (element.hasNamespace) {
        val nsRef = resolvePlainQualifier(element.plainQualifier!!) ?: return null
        return resolveDeclarationIn(nsRef, element.name!!)
    }

    return resolveDeclarationAbove(element) ?: run {
        // Try to resolve match enum shorthand
        val matchParent = element.parent as? JaktMatchPattern ?: return@run null
        val matchExpression = matchParent.ancestorOfType<JaktMatchExpression>()!!
        val matchTarget = matchExpression.findChildOfType<JaktExpression>()!!
        resolveEnumShorthand(matchTarget.jaktType, element.name!!)
    }
}

///////////
// TYPES //
///////////

// Types have to be treated specially because not all declarations are
// types (namely, functions).

private fun JaktPsiScope.getTypeDeclarations() = getDeclarations()
    .mapNotNull(JaktDeclaration::unwrapImport)
    .filter { it.isTypeDeclaration }

private fun resolveTypeDeclarationIn(element: PsiElement, name: String): JaktDeclaration? {
    if (element is JaktGeneric)
        element.getDeclGenericBounds().find { it.name == name }?.let { return it }
    if (element is JaktPsiScope)
        element.getTypeDeclarations().find { it.name == name }?.let { return it }
    return null
}

private fun resolveTypeDeclarationAbove(element: PsiElement, name: String): JaktDeclaration? {
    resolveTypeDeclarationIn(element, name)?.let { return it }
    val parent = element.ancestorOfType<JaktPsiScope>()
        ?: return element.jaktProject.findPreludeTypeDeclaration(name)
    return resolveTypeDeclarationAbove(parent, name)
}

fun resolvePlainType(element: JaktPlainType): JaktDeclaration? {
    val qual = element.plainQualifier

    return if (qual.hasNamespace) {
        val nsRef = qual.plainQualifier!!.reference?.resolve() ?: return null
        resolveTypeDeclarationIn(nsRef, qual.name!!)
    } else {
        resolveTypeDeclarationAbove(element, element.name!!) ?: run {
            // Try to resolve is enum shorthand
            val postfixParent = element.parent as? JaktUnaryExpression ?: return@run null
            val exprType = postfixParent.expression.takeIf { postfixParent.keywordIs != null }?.jaktType
                ?: return@run null
            resolveEnumShorthand(exprType, element.name!!)
        }
    }
}

//////////
// MISC //
//////////

fun resolveAccess(element: JaktAccess): JaktDeclaration? {
    val accessExpr = element.ancestorOfType<JaktAccessExpression>()!!
    val baseType = TypeInference.inferType(accessExpr.expression).resolveToBuiltinType(element.project)
    val baseDecl = (baseType as? Type.TopLevelDecl)?.declaration ?: return null
    return resolveDeclarationIn(baseDecl, element.name!!)
}
