package org.serenityos.jakt.index

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.serenityos.jakt.psi.api.JaktStructDeclaration
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.stubs.JaktFileStub

class JaktStructElementIndex : StringStubIndexExtension<JaktNamedElement>() {
    override fun getVersion() = JaktFileStub.Type.stubVersion
    override fun getKey() = KEY

    companion object {
        val KEY = StubIndexKey.createIndexKey<String, JaktNamedElement>("org.serenityos.jakt.JaktStructElementIndex")
    }
}
