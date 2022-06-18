package org.serenityos.jakt.plugin.type

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.intellij.sdk.language.psi.JaktAccess
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.JaktPlainType
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.project.jaktProject
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration
import org.serenityos.jakt.plugin.psi.declaration.JaktGeneric
import org.serenityos.jakt.plugin.psi.declaration.JaktImportBraceEntryMixin
import org.serenityos.jakt.plugin.psi.declaration.JaktImportStatementMixin
import org.serenityos.jakt.utils.ancestorOfType
import org.serenityos.jakt.utils.ancestorsOfType
import org.serenityos.jakt.utils.findChildrenOfType

//////////////////////////////
// Reference -> Declaration //
//////////////////////////////

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

fun resolvePlainQualifier(qualifier: JaktPlainQualifier): JaktDeclaration? {
    return if (qualifier.namespaceQualifierList.isNotEmpty()) {
        val nsRef = qualifier.namespaceQualifierList.last().reference?.resolve() ?: return null
        resolveDeclarationIn(nsRef, qualifier.name!!)
    } else {
        resolveDeclarationAbove(qualifier)
    }
}

fun resolvePlainType(plainType: JaktPlainType): Type {
    val idents = plainType.findChildrenOfType(JaktTypes.IDENTIFIER).map { it.text }

    if (idents.size == 1) {
        Type.Primitive.values().find { it.typeRepr() == idents[0] }?.let { return it }
    }

    var type = resolveDeclarationAbove(plainType, idents.first())?.jaktType ?: return Type.Unknown

    for (qualifier in idents.drop(1))
        type = resolveDeclarationIn(type, qualifier)

    return type
}

fun resolveAccess(access: JaktAccess): JaktDeclaration? {
    val accessExpr = access.ancestorOfType<JaktAccessExpression>()!!
    val baseType = TypeInference.inferType(accessExpr.expression).resolveToBuiltinType(access.project)
    val baseDecl = (baseType as? Type.TopLevelDecl)?.declaration ?: return null
    return resolveDeclarationIn(baseDecl, access.name!!)
}

///////////////////////////////
// Declaration -> References //
///////////////////////////////

// TODO: This needs some work
fun resolveReferencesIn(scope: PsiElement, name: String): List<PsiElement> {
    val references = mutableListOf<PsiElement>()

    walkPsiElement(scope) {
        when {
            it is JaktDeclaration && it.name == name -> {
                // If this shadows the declaration we're looking for, this
                // scope cannot have any more references to the element
                return@walkPsiElement WalkResult.SkipSiblings
            }
            it is JaktPlainQualifier && it.name == name -> references.add(it)
        }

        WalkResult.Continue
    }

    return references
}
