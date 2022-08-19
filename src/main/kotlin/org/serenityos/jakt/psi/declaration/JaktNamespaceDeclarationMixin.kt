package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.serenityos.jakt.psi.api.JaktNamespaceBody
import org.serenityos.jakt.psi.api.JaktNamespaceDeclaration
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.psi.named.JaktStubbedNamedElement
import org.serenityos.jakt.stubs.JaktNamespaceDeclarationStub
import org.serenityos.jakt.type.DeclarationType
import org.serenityos.jakt.type.NamespaceType
import org.serenityos.jakt.type.Type

abstract class JaktNamespaceDeclarationMixin : JaktStubbedNamedElement<JaktNamespaceDeclarationStub>, JaktNamespaceDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: JaktNamespaceDeclarationStub, type: IStubElementType<*, *>) : super(stub, type)

    override val jaktType: Type
        get() {
            val members = namespaceBody.members.mapNotNull {
                (it as? JaktDeclaration)?.jaktType as? DeclarationType ?: return@mapNotNull null
            }

            return NamespaceType(name, members).also { ns ->
                ns.members.forEach {
                    it.namespace = ns
                }
            }
        }

    override fun getDeclarations() = namespaceBody.members
}

val JaktNamespaceBody.members: List<JaktDeclaration>
    get() = findChildrenOfType()
