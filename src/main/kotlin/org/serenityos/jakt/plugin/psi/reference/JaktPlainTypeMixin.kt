package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktPlainType
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolveDeclarationAbove
import org.serenityos.jakt.plugin.type.resolvePlainType
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktPlainTypeMixin(node: ASTNode) : JaktNamedElement(node), JaktPlainType {
    override val jaktType: Type
        get() = resolvePlainType(this)

    override fun getNameIdentifier(): PsiElement = findChildrenOfType(JaktTypes.IDENTIFIER).last()

    override fun getReference() = singleRef(::resolveDeclarationAbove)
}
