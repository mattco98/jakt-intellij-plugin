package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.*
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktExternStructDeclarationMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktExternStructDeclaration {
    // TODO: Deduplicate with JaktStructDeclarationMixin
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val header = structHeader

            val typeParameters = if (header.genericBounds != null) {
                header.genericBounds!!.findChildrenOfType<JaktPlainQualifier>().map {
                    it.identifier.text
                }
            } else emptyList()

            // TODO: Visibility
            val methods = externStructMemberList.mapNotNull { func ->
                // TODO: Can extern structs have fields?
                if (func.structField != null)
                    return@mapNotNull null

                Type.Function(
                    func.identifier!!.text,
                    func.genericBounds?.plainQualifierList?.map { it.text!! } ?: emptyList(),
                    null,
                    func.parameterList.map {
                        Type.Function.Parameter(
                            it.identifier.text,
                            it.typeAnnotation.jaktType,
                            it.anonKeyword != null,
                            it.mutableKeyword != null,
                        )
                    },
                    func.functionReturnType?.type?.jaktType ?: Type.Primitive.Void,
                )
            }.associateBy { it.name }

            val type = Type.Struct(
                header.identifier.text,
                typeParameters,
                emptyMap(),
                methods,
            )

            // Populate our methods' thisParameters, if necessary
            methods.values.forEach {
                if (it.hasThis && it.thisParameter == null) {
                    it.thisParameter = Type.Function.Parameter(
                        "this",
                        type,
                        false,
                        it.thisIsMutable,
                    )
                }
            }

            // TODO: Better caching
            CachedValueProvider.Result(type, this)
        }

    override fun getNameIdentifier() = structHeader.identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}
