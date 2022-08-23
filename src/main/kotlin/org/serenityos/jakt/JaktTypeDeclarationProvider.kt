package org.serenityos.jakt

import com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider
import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.JaktTypeable
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import org.serenityos.jakt.psi.declaration.isTypeDeclaration
import org.serenityos.jakt.type.BoundType
import org.serenityos.jakt.type.EnumVariantType
import org.serenityos.jakt.type.FunctionType
import org.serenityos.jakt.type.resolveToBuiltinType

class JaktTypeDeclarationProvider : TypeDeclarationProvider {
    override fun getSymbolTypeDeclarations(symbol: PsiElement): Array<PsiElement>? {
        if ((symbol as? JaktDeclaration)?.isTypeDeclaration == true)
            return null

        val symbolType = (symbol as? JaktTypeable)?.jaktType ?: return null

        val type = BoundType.withInner(symbolType) {
            when (it) {
                is FunctionType -> it.returnType
                is EnumVariantType -> it.parentType
                else -> it
            }
        }

        val decl = type.resolveToBuiltinType(symbol.project).psiElement ?: return null
        return arrayOf(decl)
    }
}
