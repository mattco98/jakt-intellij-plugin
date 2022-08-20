package org.serenityos.jakt.stubs

import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.JaktLanguage

class JaktFileStub(file: JaktFile?): PsiFileStubImpl<JaktFile>(file) {
    object Type : IStubFileElementType<JaktFileStub>(JaktLanguage) {
        // Should be incremented when lexer, parser, or stub tree changes
        override fun getStubVersion() = 4
    }
}
