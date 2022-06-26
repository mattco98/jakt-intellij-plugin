package org.serenityos.jakt

import com.intellij.codeInsight.template.EverywhereContextType
import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import kotlin.reflect.KClass

@Suppress("unused")
sealed class JaktTemplateContextType(
    id: String,
    name: String,
    contextType: KClass<out TemplateContextType>
) : TemplateContextType(id, name, contextType.java) {
    class File : JaktTemplateContextType("JAKT_FILE", "File", EverywhereContextType::class) {
        override fun isInContext(context: TemplateActionContext) = context.file.language == JaktLanguage
    }

    class Declaration : JaktTemplateContextType("JAKT_DECLARATION", "Declaration", File::class) {
        override fun isInContext(context: TemplateActionContext) =
            context.file.findElementAt(context.startOffset)?.ancestorOfType<JaktDeclaration>() !is JaktFile
    }
}
