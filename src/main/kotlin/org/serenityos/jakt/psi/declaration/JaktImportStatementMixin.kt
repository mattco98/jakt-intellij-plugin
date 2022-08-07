package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.api.JaktImportStatement
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.psi.reference.singleRef
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.UnknownType

abstract class JaktImportStatementMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktImportStatement {

    override val jaktType: Type
        get() = resolveFile()?.jaktType ?: UnknownType

    override fun getReference() = singleRef { resolveFile() }
}

val JaktImportStatement.nameIdent: PsiElement
    get() = originalElement.findChildrenOfType(JaktTypes.IDENTIFIER).first()

val JaktImportStatement.aliasIdent: PsiElement?
    get() = originalElement.findChildrenOfType(JaktTypes.IDENTIFIER).getOrNull(1)

fun JaktImportStatement.resolveFile(): JaktFile? =
    jaktProject.resolveImportedFile(containingFile.originalFile.virtualFile, nameIdent.text)
