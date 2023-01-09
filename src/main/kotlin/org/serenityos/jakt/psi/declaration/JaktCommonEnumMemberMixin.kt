package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktCommonEnumMember
import org.serenityos.jakt.psi.api.JaktEnumDeclaration
import org.serenityos.jakt.psi.api.JaktFieldAccessExpression
import org.serenityos.jakt.psi.api.JaktStructDeclaration
import org.serenityos.jakt.psi.api.JaktStructField
import org.serenityos.jakt.psi.named.JaktStubbedNamedElement
import org.serenityos.jakt.psi.reference.multiRef
import org.serenityos.jakt.stubs.JaktCommonEnumMemberStub
import org.serenityos.jakt.stubs.JaktStructFieldStub
import org.serenityos.jakt.type.Type

abstract class JaktCommonEnumMemberMixin : JaktStubbedNamedElement<JaktCommonEnumMemberStub>, JaktCommonEnumMember {
    override val jaktType: Type
        get() = typeAnnotation.jaktType

    constructor(node: ASTNode) : super(node)
    constructor(stub: JaktCommonEnumMemberStub, type: IStubElementType<*, *>) : super(stub, type)
}