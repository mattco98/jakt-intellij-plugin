package org.serenityos.jakt.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktImportStatement
import org.intellij.sdk.language.psi.JaktNamespaceQualifier
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.JaktPsiFactory
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.declaration.JaktImportStatementMixin

class ImportNSDeclarationIntention : JaktIntention<ImportNSDeclarationIntention.Context>("Add import for member") {
    override fun getContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val file = element.containingFile as? JaktFile ?: return null
        val qualifier = element.ancestorOfType<JaktPlainQualifier>(withSelf = true) ?: return null
        val namespaces = qualifier.namespaceQualifierList
        if (namespaces.isEmpty())
            return null

        val qualifierPart = if (element == qualifier.identifier) {
            if (namespaces.size == 1) {
                QualifierPart.Identifier(qualifier)
            } else return null
        } else {
            val ns = element.ancestorOfType<JaktNamespaceQualifier>(withSelf = true) ?: return null
            namespaces.indexOf(ns).let {
                // if (it == -1) return null else QualifierPart.Namespace(qualifier, it)
                if (it != 1) return null else QualifierPart.Namespace(qualifier, it)
            }
        }

        val resolvedMember = qualifierPart.resolve() ?: return null
        val resolvedFile = resolvedMember.containingFile
        if (resolvedFile == file)
            return null

        // Find the import statement. This isn't so straightforward, since reference
        // unwrap import statements

        val importStatement = file.getDeclarations().filterIsInstance<JaktImportStatementMixin>()
            .find { it.nameIdent.text == resolvedFile.name.substringBefore(".jakt") }
            ?: return null

        description = "Add import for \"${qualifierPart.text()}\""

        return Context(importStatement, qualifierPart)
    }

    override fun apply(project: Project, editor: Editor, context: Context) {
        val factory = JaktPsiFactory(project)

        // First step: Replace the qualifier
        context.qualifierPart.replace(project, factory)

        // Second step: Modify the import statement
        val import = context.importStatement
        val importText = import.text
        val newImportName = context.qualifierPart.targetText()

        val newImportText = if (import.importBraceList?.importBraceEntryList?.isEmpty() != false) {
            buildString {
                append("import ")
                append(import.nameIdent.text)
                append(" ")
                val alias = import.aliasIdent?.text
                if (alias != null)
                    append("as $alias ")
                append("{ $newImportName }")
            }
        } else {
            val indexOfLast = import.importBraceList!!.importBraceEntryList.last().textRange.endOffset
            importText.substring(0, indexOfLast) + ", $newImportName }"
        }

        val newStatement = factory.createFromText<JaktImportStatement>(newImportText)
            ?: error("Failed to create import statement")

        context.importStatement.replace(newStatement)
    }

    sealed class QualifierPart(protected val qualifier: JaktPlainQualifier) {
        abstract fun resolve(): PsiElement?

        abstract fun namespaceText(): String

        abstract fun targetText(): String

        fun text() = "${namespaceText()}::${targetText()}"

        abstract fun replace(project: Project, factory: JaktPsiFactory)

        class Namespace(
            qualifier: JaktPlainQualifier,
            private val index: Int = qualifier.namespaceQualifierList.lastIndex,
        ) : QualifierPart(qualifier) {
            override fun resolve() = qualifier.namespaceQualifierList.getOrNull(index)?.reference?.resolve()

            override fun namespaceText() = qualifier.namespaceQualifierList
                .subList(0, index)
                .joinToString(separator = "::") { it.identifier.text }

            override fun targetText(): String = qualifier.namespaceQualifierList[index].identifier.text

            override fun replace(project: Project, factory: JaktPsiFactory) {
                val newIdent = buildString {
                    for (ns in qualifier.namespaceQualifierList.drop(1)) {
                        append(ns.identifier.text)
                        append("::")
                    }
                    append(qualifier.identifier.text)
                }
                qualifier.replace(factory.createPlainQualifier(newIdent))
            }
        }

        class Identifier(qualifier: JaktPlainQualifier) : QualifierPart(qualifier) {
            override fun resolve() = qualifier.reference?.resolve()

            override fun namespaceText() = Namespace(qualifier).text()

            override fun targetText(): String = qualifier.identifier.text

            override fun replace(project: Project, factory: JaktPsiFactory) {
                qualifier.replace(factory.createPlainQualifier(qualifier.identifier.text))
            }
        }
    }

    data class Context(
        val importStatement: JaktImportStatementMixin,
        val qualifierPart: QualifierPart,
    )
}
