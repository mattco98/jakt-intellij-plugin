package org.serenityos.jakt.psi.declaration

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.serenityos.jakt.psi.api.JaktExternImport
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.stubs.JaktExternImportStub

abstract class JaktExternImportMixin : StubBasedPsiElementBase<JaktExternImportStub>, JaktExternImport {
    constructor(node: ASTNode) : super(node)
    constructor(stub: JaktExternImportStub, type: IStubElementType<*, *>) : super(stub, type)

    override fun getDeclarations(): List<JaktDeclaration> = members
}

val JaktExternImport.members: List<JaktDeclaration>
    get() = findChildrenOfType()
