package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktTraitDeclaration
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.FunctionType
import org.serenityos.jakt.type.TraitType
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktTraitDeclarationMixin(
    node: ASTNode
) : JaktNamedElement(node), JaktTraitDeclaration {
    override val jaktType by recursivelyGuarded {
        val functions = mutableListOf<FunctionType>()

        producer {
            TraitType(identifier.text, functions)
        }

        initializer {
            functions.addAll(functionDeclarationList.map { it.jaktType }.filterIsInstance<FunctionType>())
        }
    }

    override fun getNameIdentifier() = identifier

    override fun getDeclarations(): List<JaktDeclaration> = functionDeclarationList
}
