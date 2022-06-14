package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.sdk.language.psi.JaktFieldAccessExpression
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.JaktStructField
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.reference.JaktRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktStructFieldMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktStructField {
    override val jaktType: Type
        get() = typeAnnotation.jaktType

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = Ref(this)

    class Ref(element: JaktStructField) : JaktRef<JaktStructField>(element) {
        override fun multiResolve(): List<PsiElement> {
            val references = mutableListOf<PsiElement>()

            element.ancestorOfType<JaktStructDeclaration>()!!.structBody.structMemberList.forEach {
                val function = it.functionDeclaration ?: return@forEach
                PsiTreeUtil.processElements(function) { el ->
                    if (el is JaktFieldAccessExpression && el.name == element.name)
                        references.add(el)
                    true
                }
            }

            return references
        }
    }
}
