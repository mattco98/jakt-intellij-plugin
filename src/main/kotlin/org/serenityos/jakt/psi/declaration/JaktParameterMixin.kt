package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.serenityos.jakt.psi.api.JaktParameter
import org.serenityos.jakt.psi.named.JaktStubbedNamedElement
import org.serenityos.jakt.stubs.JaktParameterStub
import org.serenityos.jakt.type.Type

abstract class JaktParameterMixin : JaktStubbedNamedElement<JaktParameterStub>, JaktParameter {
    override val jaktType: Type
        get() = typeAnnotation.jaktType

    constructor(node: ASTNode) : super(node)
    constructor(stub: JaktParameterStub, type: IStubElementType<*, *>) : super(stub, type)
}
