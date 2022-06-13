package org.serenityos.jakt.plugin.psi.misc

import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktImportStatement
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.JaktFile
import org.serenityos.jakt.plugin.project.jaktProject
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.reference.JaktRef
import org.serenityos.jakt.plugin.type.Type

abstract class JaktImportStatementMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktImportStatement {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            CachedValueProvider.Result(resolveFile()?.jaktType ?: Type.Unknown, this)
        }

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    fun resolveFile(): JaktFile? = jaktProject.resolveImportedFile(containingFile.virtualFile, name)

    override fun getReference() = Ref(this)

    class Ref(element: JaktImportStatement) : JaktRef<JaktImportStatement>(element) {
        override fun multiResolve(): List<JaktPsiElement> {
            val file = element.jaktProject.resolveImportedFile(element.containingFile.virtualFile, element.name!!)
            return listOfNotNull(file)
        }
    }
}
