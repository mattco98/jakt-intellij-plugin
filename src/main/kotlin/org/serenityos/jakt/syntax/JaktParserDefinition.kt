package org.serenityos.jakt.syntax

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.JaktLanguage
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.parser.JaktParser

class JaktParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = JaktLexerAdapter()
    override fun createParser(project: Project?): PsiParser = JaktParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun createElement(node: ASTNode?): PsiElement = JaktTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = JaktFile(viewProvider)

    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES
    override fun getStringLiteralElements(): TokenSet = STRING_LITERALS

    companion object {
        val FILE = IFileElementType(JaktLanguage)

        private val WHITE_SPACES = TokenSet.create(JaktLexerBase.SPACE)
        private val COMMENTS = TokenSet.create(JaktTypes.COMMENT, JaktTypes.DOC_COMMENT)
        private val STRING_LITERALS = TokenSet.create(JaktTypes.STRING_LITERAL)
    }
}
