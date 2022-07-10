package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktForDecl
import org.intellij.sdk.language.psi.JaktForStatement
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.caching.typeCache
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.*

abstract class JaktForDeclMixin(node: ASTNode) : JaktNamedElement(node), JaktForDecl {
    override val jaktType: Type
        get() = typeCache().resolveWithCaching(this) {
            val forStatement = ancestorOfType<JaktForStatement>() ?: return@resolveWithCaching UnknownType
            val iteratorType = forStatement.expression.jaktType

            BoundType.withInner(iteratorType) {
                if (it is StructType) {
                    when (val type = it.methods["next"]?.returnType) {
                        is OptionalType -> type.underlyingType
                        null -> UnknownType
                        else -> type
                    }

                } else UnknownType
            }
        }
}
