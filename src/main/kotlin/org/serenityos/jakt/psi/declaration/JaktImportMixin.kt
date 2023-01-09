package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.api.JaktImport
import org.serenityos.jakt.psi.api.JaktImportAs
import org.serenityos.jakt.psi.api.JaktImportPath
import org.serenityos.jakt.psi.findChildOfType
import org.serenityos.jakt.psi.findChildrenOfType
import org.serenityos.jakt.psi.named.JaktStubbedNamedElement
import org.serenityos.jakt.psi.reference.singleRef
import org.serenityos.jakt.stubs.JaktImportStub
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.utils.unreachable

abstract class JaktImportMixin : JaktStubbedNamedElement<JaktImportStub>, JaktImport {
    constructor(node: ASTNode) : super(node)
    constructor(stub: JaktImportStub, type: IStubElementType<*, *>) : super(stub, type)

    override val jaktType: Type
        get() = unreachable()

    override fun getReference() = singleRef { resolveFile() }
}

val JaktImport.nameIdent: PsiElement
    get() = originalElement.findChildOfType<JaktImportPath>()!!.findChildrenOfType(JaktTypes.IDENTIFIER).last()

val JaktImport.aliasIdent: PsiElement?
    get() = originalElement.findChildOfType<JaktImportAs>()?.findChildrenOfType(JaktTypes.IDENTIFIER)?.last()

fun JaktImport.resolveFile(): JaktFile? =
    jaktProject.resolveImportedFile(containingFile.originalFile.virtualFile, nameIdent.text)
