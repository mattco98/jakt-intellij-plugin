package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktImportBraceEntry
import org.intellij.sdk.language.psi.JaktImportStatement
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.psi.reference.singleRef
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.resolveDeclarationIn

abstract class JaktImportBraceEntryMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktImportBraceEntry {
    override val jaktType: Type
        get() {
            val importType = ancestorOfType<JaktImportStatement>()?.jaktType as? Type.Namespace
            return importType?.members?.firstOrNull { it.name == name } ?: Type.Unknown
        }

    override fun getReference() = singleRef {
        val file = it.ancestorOfType<JaktImportStatement>()?.reference?.resolve() as? JaktFile
            ?: return@singleRef null
        resolveDeclarationIn(file, it.nameNonNull)
    }
}

fun JaktImportBraceEntry.resolveElement() = ancestorOfType<JaktImportStatement>()?.resolveFile()?.let {
    resolveDeclarationIn(it, nameNonNull)
}
