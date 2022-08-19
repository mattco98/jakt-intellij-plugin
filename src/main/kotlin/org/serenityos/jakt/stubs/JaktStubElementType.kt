package org.serenityos.jakt.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import org.serenityos.jakt.JaktLanguage
import org.serenityos.jakt.index.JaktNamedElementIndex
import org.serenityos.jakt.psi.named.JaktNamedStub

abstract class JaktStubElementType<StubT : StubElement<*>, PsiT : PsiElement>(
    debugName: String,
) : IStubElementType<StubT, PsiT>(debugName, JaktLanguage) {
    final override fun getExternalId() = "jakt.${super.toString()}"

    override fun indexStub(stub: StubT, sink: IndexSink) {}
}

abstract class JaktNamedStubElementType<StubT, PsiT>(debugName: String) : JaktStubElementType<StubT, PsiT>(debugName)
    where StubT : StubElement<*>, StubT : JaktNamedStub, PsiT : PsiElement {
    override fun indexStub(stub: StubT, sink: IndexSink) {
        stub.name?.let {
            sink.occurrence(JaktNamedElementIndex.KEY, it)
        }
    }
}
