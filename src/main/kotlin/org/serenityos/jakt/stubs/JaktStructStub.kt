package org.serenityos.jakt.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.serenityos.jakt.psi.api.JaktStructDeclaration
import org.serenityos.jakt.psi.impl.JaktStructDeclarationImpl
import org.serenityos.jakt.psi.named.JaktNamedStub

class JaktStructDeclarationStub(
    parent: StubElement<*>?,
    type: IStubElementType<*, *>,
    override val name: String?,
) : StubBase<JaktStructDeclaration>(parent, type), JaktNamedStub {
    object Type : JaktStubElementType<JaktStructDeclarationStub, JaktStructDeclaration>("STRUCT_DECLARATION") {
        override fun serialize(stub: JaktStructDeclarationStub, dataStream: StubOutputStream) = with(dataStream) {
            writeName(stub.name)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): JaktStructDeclarationStub
            = JaktStructDeclarationStub(parentStub, Type, dataStream.readNameString())

        override fun createStub(
            psi: JaktStructDeclaration,
            parentStub: StubElement<out PsiElement>?
        ) = JaktStructDeclarationStub(parentStub, Type, psi.name)

        override fun createPsi(stub: JaktStructDeclarationStub) = JaktStructDeclarationImpl(stub, Type)
    }
}
