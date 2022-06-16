package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.JaktTopLevelDefinition
import org.intellij.sdk.language.psi.impl.JaktExpressionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.declaration.JaktImportBraceEntryMixin
import org.serenityos.jakt.plugin.psi.declaration.JaktNameIdentifierOwner
import org.serenityos.jakt.plugin.psi.declaration.JaktImportStatementMixin
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.TypeInference
import org.serenityos.jakt.utils.ancestorPairsOfType
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktPlainQualifierMixin(node: ASTNode) : JaktExpressionImpl(node), JaktPlainQualifier {
    override val jaktType: Type
        get() = (reference.resolve() as? JaktTypeable)?.jaktType ?: Type.Unknown

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = Ref(this)

    class Ref(element: JaktPlainQualifier) : JaktRef<JaktPlainQualifier>(element) {
        override fun multiResolve(): List<PsiElement> {
            val nsRef = element.namespaceQualifierList.lastOrNull()?.reference?.resolve()
            return if (nsRef != null) {
                listOfNotNull(nsRef.findTopLevelDecls().firstOrNull {
                    it.name == element.name
                })
            } else {
                listOfNotNull(resolvePlainQualifier(element))
            }
        }
    }
}

private fun PsiElement.findTopLevelDecls(): List<JaktTopLevelDefinition> = when (this) {
    is JaktStructDeclaration -> structBody.structMemberList.mapNotNull { it.functionDeclaration }
    else -> findChildrenOfType()
}

private fun PsiElement.unwrapImport(): PsiElement = when (this) {
    is JaktImportStatementMixin -> resolveFile() ?: this
    is JaktImportBraceEntryMixin -> resolveElement() ?: this
    else -> this
}

fun resolvePlainQualifier(element: JaktNameIdentifierOwner): PsiElement? {
    for ((current, parent) in element.ancestorPairsOfType<JaktPsiScope>()) {
        return parent.findDeclarationIn(element.name!!, current)?.unwrapImport() ?: continue
    }

    return null
}
