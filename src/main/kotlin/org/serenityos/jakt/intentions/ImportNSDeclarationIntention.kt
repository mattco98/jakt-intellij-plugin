package org.serenityos.jakt.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.JaktPsiFactory
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktImport
import org.serenityos.jakt.psi.api.JaktPlainQualifier
import org.serenityos.jakt.psi.declaration.aliasString
import org.serenityos.jakt.psi.declaration.targetString
import org.serenityos.jakt.psi.reference.index

class ImportNSDeclarationIntention : JaktIntention<ImportNSDeclarationIntention.Context>("Add import for member") {
    override fun getContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val file = element.containingFile as? JaktFile ?: return null
        val qualifier = element.ancestorOfType<JaktPlainQualifier>(withSelf = true) ?: return null

        // Jakt current does not support deeply-nested imports
        if (qualifier.index != 1)
            return null

        val resolvedMember = qualifier.reference?.resolve() ?: return null
        val resolvedFile = resolvedMember.containingFile
        if (resolvedFile == file)
            return null

        val importStatement = file.getDeclarations().filterIsInstance<JaktImport>()
            .find { it.targetString == resolvedFile.name.substringBefore(".jakt") }
            ?: return null

        description = "Add import for \"${qualifier.text}\""

        return Context(importStatement, qualifier)
    }

    override fun apply(project: Project, editor: Editor, context: Context) {
        val factory = JaktPsiFactory(project)

        // First step: Replace the qualifier
        context.qualifier.replace(
            factory.createIdentifier(context.qualifier.text.substringBeforeLast("::"))
        )

        // Second step: Modify the import statement
        val import = context.importStatement
        val importText = import.text
        val newImportName = context.qualifier.identifier.text

        val newImportText = if (import.importBraceList?.importBraceEntryList?.isEmpty() != false) {
            buildString {
                append("import ")
                append(import.targetString)
                append(" ")
                val alias = import.aliasString
                if (alias != null)
                    append("as $alias ")
                append("{ $newImportName }")
            }
        } else {
            val indexOfLast = import.importBraceList!!.importBraceEntryList.last().textRange.endOffset
            importText.substring(0, indexOfLast) + ", $newImportName }"
        }

        val newStatement = factory.createFromText<JaktImport>(newImportText)
            ?: error("Failed to create import statement")

        context.importStatement.replace(newStatement)
    }

    data class Context(
        val importStatement: JaktImport,
        val qualifier: JaktPlainQualifier,
    )
}
