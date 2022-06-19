package org.serenityos.jakt.plugin

import com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider
import com.intellij.psi.PsiElement
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveToBuiltinType

class JaktTypeDeclarationProvider : TypeDeclarationProvider {
    override fun getSymbolTypeDeclarations(symbol: PsiElement): Array<PsiElement>? {
        val symbolType = (symbol as? JaktTypeable)?.jaktType ?: return null

        val type = when (symbolType) {
            is Type.Function -> symbolType.returnType
            is Type.EnumVariant -> symbolType.parent
            else -> symbolType
        }

        val decl = (type.resolveToBuiltinType(symbol.project) as? Type.TopLevelDecl)?.declaration ?: return null
        return arrayOf(decl)
    }
}
