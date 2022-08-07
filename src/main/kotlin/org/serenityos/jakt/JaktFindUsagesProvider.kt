package org.serenityos.jakt

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import org.serenityos.jakt.psi.api.*
import org.serenityos.jakt.psi.declaration.isClass
import org.serenityos.jakt.psi.declaration.isExtern
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.render.renderElement
import org.serenityos.jakt.syntax.JaktLexerAdapter
import org.serenityos.jakt.utils.unreachable

class JaktFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            JaktLexerAdapter(),
            TokenSet.create(JaktTypes.IDENTIFIER),
            TokenSet.create(JaktTypes.COMMENT, JaktTypes.DOC_COMMENT),
            TokenSet.create(
                JaktTypes.STRING_LITERAL,
                JaktTypes.CHAR_LITERAL,
                JaktTypes.BYTE_CHAR_LITERAL,
            ),
        )
    }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        if (psiElement is JaktThisExpression || psiElement is JaktThisParameter)
            return false

        return psiElement is PsiNamedElement
    }

    override fun getHelpId(psiElement: PsiElement) = HelpID.FIND_OTHER_USAGES

    override fun getType(element: PsiElement): String {
        return when (element) {
            is JaktArgument -> if (element.labeledArgument != null) "labeled argument" else unreachable()
            is JaktCallExpression -> "function call"
            is JaktFunction -> (if (element.isExtern) "extern " else "") + "function"
            is JaktParameter -> if (element.anonKeyword != null) "anonymous parameter" else "parameter"
            is JaktNamespaceDeclaration -> "namespace"
            is JaktStructDeclaration -> buildString {
                if (element.isExtern)
                    append("extern ")

                if (element.isClass) {
                    append("class")
                } else {
                    append("struct")
                }
            }
            is JaktStructField -> "struct field"
            is JaktEnumDeclaration -> "enum"
            is JaktEnumVariant -> "enum variant"
            is JaktGenericBound -> "generic parameter"
            is JaktImportBraceEntry -> "import item"
            is JaktVariableDeclarationStatement,
            is JaktVariableDecl,
            is JaktForDecl,
            is JaktCatchDecl,
            is JaktDestructuringLabel -> "variable declaration"
            else -> "TODO: JaktFindUsagesProvider::getType for ${element::class.simpleName}"
        }
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is JaktArgument -> element.labeledArgument!!.identifier.text
            is JaktNamedElement -> element.name ?: return "ERROR"
            else -> "TODO(getDescriptiveName => ${element::class.simpleName})"
        }
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean) = renderElement(element)
}
