package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktImportBraceEntry
import org.intellij.sdk.language.psi.JaktImportStatement
import org.serenityos.jakt.plugin.JaktFile
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.reference.JaktRef
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.ancestorOfType

abstract class JaktImportBraceEntryMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktImportBraceEntry {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val importType = ancestorOfType<JaktImportStatement>()?.jaktType as? Type.Namespace
            val member = importType?.members?.firstOrNull { it.name == name }

            CachedValueProvider.Result(member ?: Type.Unknown, this)
        }

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    fun resolveElement() = ancestorOfType<JaktImportStatementMixin>()?.resolveFile()?.findDeclarationIn(name)

    override fun getReference() = Ref(this)

    class Ref(element: JaktImportBraceEntry) : JaktRef<JaktImportBraceEntry>(element) {
        override fun multiResolve(): List<PsiElement> {
            val file = element.ancestorOfType<JaktImportStatement>()?.reference?.resolve() as? JaktFile
            return listOfNotNull(file?.findDeclarationIn(element.name))
        }
    }
}
