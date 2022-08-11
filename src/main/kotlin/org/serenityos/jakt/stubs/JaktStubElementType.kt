package org.serenityos.jakt.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import org.serenityos.jakt.JaktLanguage

abstract class JaktStubElementType<StubT : StubElement<*>, PsiT : PsiElement>(
    debugName: String,
) : IStubElementType<StubT, PsiT>(debugName, JaktLanguage) {
    final override fun getExternalId() = "jakt.${super.toString()}"

    override fun indexStub(stub: StubT, sink: IndexSink) {
        // TODO
    }
}
