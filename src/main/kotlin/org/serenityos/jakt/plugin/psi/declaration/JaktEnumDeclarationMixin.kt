package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktGenericBound
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type

abstract class JaktEnumDeclarationMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktEnumDeclaration {
    override val jaktType: Type
        get() {
            val typeParameters = getDeclGenericBounds().map { Type.TypeVar(it.name) }

            // TODO: Variants
            return Type.Enum(
                name,
                underlyingTypeEnumBody?.typeAnnotation?.jaktType as? Type.Primitive,
                emptyMap(),
            ).let {
                it.declaration = this

                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }
        }

    override fun getDeclGenericBounds(): List<JaktGenericBound> = normalEnumBody?.genericBounds?.genericBoundList ?: emptyList()

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getTextOffset(): Int = nameIdentifier.textOffset
}
