package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktImportBraceEntry
import org.intellij.sdk.language.psi.JaktImportStatement
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.psi.reference.singleRef
import org.serenityos.jakt.type.JaktResolver
import org.serenityos.jakt.type.NamespaceType
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.UnknownType

abstract class JaktImportBraceEntryMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktImportBraceEntry {
    override val jaktType: Type
        get() {
            val importType = ancestorOfType<JaktImportStatement>()?.jaktType as? NamespaceType
            return importType?.members?.firstOrNull { it.name == name } ?: UnknownType
        }

    override fun getReference() = singleRef { resolveElement() }
}

fun JaktImportBraceEntry.resolveElement() = ancestorOfType<JaktImportStatement>()?.resolveFile()?.let { file ->
    JaktResolver(file).findDeclaration(nameNonNull) { it !is JaktImportStatement }
}
