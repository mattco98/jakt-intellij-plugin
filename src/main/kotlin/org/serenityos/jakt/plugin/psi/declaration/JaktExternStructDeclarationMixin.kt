package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.*
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.lexer.JaktLexer
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.specialize
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktExternStructDeclarationMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktExternStructDeclaration, JaktPsiScope {
    // TODO: Deduplicate with JaktStructDeclarationMixin
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val header = structHeader
            val structName = header.identifier.text

            val typeParameters = if (header.genericBounds != null) {
                getDeclGenericBounds().map { Type.TypeVar(it.identifier.text) }
            } else emptyList()

            val methods = mutableMapOf<String, Type.Function>()

            val struct = Type.Struct(
                structName,
                emptyMap(),
                methods,
            ).let {
                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }

            // TODO: Visibility
            externStructMemberList.mapNotNull { func ->
                // TODO: Can extern structs have fields?
                if (func.structField != null)
                    return@mapNotNull null

                // TODO: Stop skipping constructors
                if (func.identifier!!.text == structName)
                    return@mapNotNull null

                Type.Function(
                    func.identifier!!.text,
                    null,
                    func.parameterList.map {
                        Type.Function.Parameter(
                            it.identifier.text,
                            it.typeAnnotation.jaktType,
                            it.anonKeyword != null,
                            it.mutKeyword != null,
                        )
                    },
                    func.functionReturnType?.type?.jaktType ?: Type.Primitive.Void,
                ).also {
                    if (func.thisParameter != null) {
                        it.hasThis = true
                        it.thisIsMutable = func.thisParameter!!.mutKeyword != null
                    }
                }
            }.forEach {
                methods[it.name] = it
            }

            // Populate our methods' thisParameters, if necessary
            methods.values.forEach {
                if (it.hasThis && it.thisParameter == null) {
                    it.thisParameter = Type.Function.Parameter(
                        "this",
                        struct,
                        false,
                        it.thisIsMutable,
                    )
                }
            }

            // TODO: Better caching
            CachedValueProvider.Result(struct, this)
        }

    override fun getDeclGenericBounds() = structHeader.genericBounds?.genericBoundList ?: emptyList()

    override fun getNameIdentifier() = structHeader.identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}
