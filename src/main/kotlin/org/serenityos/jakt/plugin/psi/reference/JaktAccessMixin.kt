package org.serenityos.jakt.plugin.psi.reference

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.sdk.language.psi.JaktAccess
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.intellij.sdk.language.psi.impl.JaktExpressionImpl
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.api.findDeclarationInOrAbove
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.ancestorOfType
import org.serenityos.jakt.utils.descendantOfType

abstract class JaktAccessMixin(
    node: ASTNode,
) : JaktExpressionImpl(node), JaktAccess {
    override val jaktType: Type
        get() = (reference.resolve() as? JaktTypeable)?.jaktType ?: Type.Unknown

    override fun getNameIdentifier(): PsiElement? = descendantOfType(JaktTypes.IDENTIFIER)

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier?.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getReference() = Ref(this)

    class Ref(element: JaktAccess) : JaktRef<JaktAccess>(element) {
        override fun multiResolve(): List<PsiElement> {
            // TODO: This is far from perfect. For example, in the following code:
            //
            //     function main() {
            //         let f = Foo()
            //         let Foo = 2
            //         f.bar()
            //     }
            //
            // f.bar(), assuming it is a valid reference, will never be resolved because
            // this code finds the local declaration of "Foo".

            val name = element.name ?: return emptyList()
            val accessExpr = element.ancestorOfType<JaktAccessExpression>() ?: return emptyList()
            val baseType = (accessExpr.expression.reference?.resolve() as? JaktTypeable)?.jaktType ?: return emptyList()
            val baseTypeName = (baseType as? Type.TopLevelDecl)?.name ?: return emptyList()
            val decl = element.findDeclarationInOrAbove(baseTypeName) ?: return emptyList()
            return listOfNotNull((decl as? JaktPsiScope)?.getDeclarations()?.find { it.name == name })
        }
    }
}
