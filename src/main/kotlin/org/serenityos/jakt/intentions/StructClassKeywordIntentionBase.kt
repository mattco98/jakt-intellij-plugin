package org.serenityos.jakt.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.JaktPsiFactory
import org.serenityos.jakt.utils.ancestorOfType

/**
 * Converts the "class" keyword to "struct" and vice-versa
 */
sealed class StructClassKeywordIntentionBase(private val isClass: Boolean) : JaktIntention<JaktStructDeclaration>(
    if (isClass) "Change to \"struct\"" else "Change to \"class\""
) {
    override fun getContext(project: Project, editor: Editor, element: PsiElement): JaktStructDeclaration? {
        if (isClass && element.elementType != JaktTypes.CLASS_KEYWORD)
            return null
        if (!isClass && element.elementType != JaktTypes.STRUCT_KEYWORD)
            return null
        return element.ancestorOfType<JaktStructDeclaration>()
    }

    override fun apply(project: Project, editor: Editor, context: JaktStructDeclaration) {
        if (isClass) {
            context.replace(JaktPsiFactory(project).createDefinition(context.text.replaceFirst("class", "struct")))
        } else {
            context.replace(JaktPsiFactory(project).createDefinition(context.text.replaceFirst("struct", "class")))
        }
    }

}

object ClassToStructKeywordIntention : StructClassKeywordIntentionBase(true)
object StructToClassKeywordIntention : StructClassKeywordIntentionBase(false)
