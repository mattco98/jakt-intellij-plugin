package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.comptime.ArrayValue
import org.serenityos.jakt.comptime.StringValue
import org.serenityos.jakt.comptime.comptimeValue
import org.serenityos.jakt.project.jaktProject
import org.serenityos.jakt.psi.api.JaktImport
import org.serenityos.jakt.psi.caching.typeCache
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

// Note that JaktImport doesn't have a type, and thus doesn't use the typeCache normally. So we
// can repurpose it to save this string
val JaktImport.targetString: String
    get() = typeCache().resolveWithCaching(this) {
        importTarget.identifier?.let { return@resolveWithCaching it.text }

        when (val comptimeValue = importTarget.callExpression?.comptimeValue) {
            is StringValue -> comptimeValue.value
            is ArrayValue -> {
                val project = jaktProject
                val containingFile = containingFile.originalFile.virtualFile
                for (value in comptimeValue.values) {
                    if (value is StringValue && project.resolveImportedFile(containingFile, value.value) != null)
                        return@resolveWithCaching value.value
                }
                "__UNKNOWN"
            }
            else -> "__UNKNOWN"
        }
    }

val JaktImport.aliasString: String?
    get() = originalElement.findChildrenOfType(JaktTypes.IDENTIFIER).firstOrNull()?.text

fun JaktImport.resolveFile(): JaktFile? =
    jaktProject.resolveImportedFile(containingFile.originalFile.virtualFile, targetString)
