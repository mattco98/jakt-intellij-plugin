package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktFieldAccessExpression
import org.serenityos.jakt.psi.api.JaktStructDeclaration
import org.serenityos.jakt.psi.api.JaktStructField
import org.serenityos.jakt.psi.named.JaktStubbedNamedElement
import org.serenityos.jakt.psi.reference.multiRef
import org.serenityos.jakt.stubs.JaktStructFieldStub
import org.serenityos.jakt.type.Type

abstract class JaktStructFieldMixin : JaktStubbedNamedElement<JaktStructFieldStub>, JaktStructField {
    override val jaktType: Type
        get() = typeAnnotation.jaktType

    constructor(node: ASTNode) : super(node)
    constructor(stub: JaktStructFieldStub, type: IStubElementType<*, *>) : super(stub, type)

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
