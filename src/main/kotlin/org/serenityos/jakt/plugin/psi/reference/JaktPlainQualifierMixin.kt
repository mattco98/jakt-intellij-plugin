package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.JaktTopLevelDefinition
import org.intellij.sdk.language.psi.impl.JaktExpressionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.declaration.JaktNameIdentifierOwner
import org.serenityos.jakt.plugin.psi.misc.JaktImportStatementMixin
import org.serenityos.jakt.utils.ancestorPairsOfType
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktPlainQualifierMixin(node: ASTNode) : JaktExpressionImpl(node), JaktPlainQualifier {
    override fun getNameIdentifier(): PsiElement = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = Ref(this)

    class Ref(element: JaktPlainQualifier) : JaktRef<JaktPlainQualifier>(element) {
        override fun multiResolve(): List<JaktPsiElement> {
            val nsRef = element.namespaceQualifierList.lastOrNull()?.reference?.resolve()?.let {
                (it as? JaktImportStatementMixin)?.resolveFile() ?: it
            }
            return if (nsRef != null) {
                listOfNotNull(nsRef.findChildrenOfType<JaktTopLevelDefinition>().firstOrNull {
                    it.name == element.name
                })
            } else {
                listOfNotNull(resolvePlainQualifier(element))
            }
        }
    }
}

fun resolvePlainQualifier(element: JaktNameIdentifierOwner): JaktPsiElement? {
    for ((current, parent) in element.ancestorPairsOfType<JaktPsiScope>())
        return parent.findDeclarationIn(element.name!!, current) ?: continue

    return null
}
