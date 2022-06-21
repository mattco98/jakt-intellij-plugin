package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.sdk.language.psi.JaktFieldAccessExpression
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.JaktStructField
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.psi.reference.multiRef
import org.serenityos.jakt.type.Type

abstract class JaktStructFieldMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktStructField {
    override val jaktType: Type
        get() = typeAnnotation.jaktType

    override fun getReference() = multiRef { field ->
        val references = mutableListOf<PsiElement>()

        field.ancestorOfType<JaktStructDeclaration>()!!.structBody.structMemberList.forEach {
            val function = it.structMethod ?: return@forEach
            PsiTreeUtil.processElements(function) { el ->
                if (el is JaktFieldAccessExpression && el.name == field.name)
                    references.add(el)
                true
            }
        }

        // TODO: Search references of parent struct

        references
    }
}
