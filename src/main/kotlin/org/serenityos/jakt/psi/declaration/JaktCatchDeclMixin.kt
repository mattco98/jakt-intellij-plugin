package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.api.JaktForDecl
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.UnknownType

abstract class JaktCatchDeclMixin(node: ASTNode) : JaktNamedElement(node), JaktForDecl {
    override val jaktType: Type
        get() = jaktProject.findPreludeDeclaration("Error")?.jaktType ?: UnknownType
}
