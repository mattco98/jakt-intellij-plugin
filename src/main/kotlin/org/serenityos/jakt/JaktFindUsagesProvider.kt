package org.serenityos.jakt

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.syntax.JaktLexerAdapter

class JaktFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            JaktLexerAdapter(),
            TokenSet.create(JaktTypes.IDENTIFIER),
            TokenSet.create(JaktTypes.COMMENT),
            TokenSet.create(
                JaktTypes.STRING_LITERAL,
                JaktTypes.CHAR_LITERAL,
                JaktTypes.BYTE_CHAR_LITERAL,
            ),
        )
    }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return psiElement is PsiNamedElement
    }

    override fun getHelpId(psiElement: PsiElement) = HelpID.FIND_OTHER_USAGES

    override fun getType(element: PsiElement): String {
        return when (element) {
            is JaktArgument -> if (element.labeledArgument != null) "labeled argument" else error("unreachable")
            is JaktCallExpression -> "function call"
            is JaktExternFunctionDeclaration -> "extern function"
            is JaktExternStructDeclaration -> "extern struct"
            is JaktFunctionDeclaration -> "function"
            is JaktNamespaceDeclaration -> "namespace"
            is JaktStructDeclaration -> if (element.structHeader.classKeyword != null) "class" else "struct"
            is JaktEnumDeclaration -> "enum"
            is JaktParameter -> if (element.anonKeyword != null) "anonymous parameter" else "parameter"
            is JaktVariableDeclarationStatement -> "variable declaration"
            else -> "TODO(getType => ${element::class.simpleName})"
        }
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is JaktArgument -> element.labeledArgument!!.identifier.text
            is JaktExternFunctionDeclaration -> element.identifier.text
            is JaktExternStructDeclaration -> element.structHeader.identifier.text
            is JaktFunctionDeclaration -> element.identifier.text
            is JaktNamespaceDeclaration -> element.identifier.text
            is JaktStructDeclaration -> element.structHeader.identifier.text
            is JaktEnumDeclaration -> element.identifier.text
            is JaktParameter -> element.identifier.text
            else -> "TODO(getDescriptiveName => ${element::class.simpleName})"
        }
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean) = getDescriptiveName(element)
}
