package org.serenityos.jakt.index

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.serenityos.jakt.psi.named.JaktNameIdentifierOwner
import org.serenityos.jakt.stubs.JaktFileStub

class JaktNamedElementIndex : StringStubIndexExtension<JaktNameIdentifierOwner>() {
    override fun getVersion() = JaktFileStub.Type.stubVersion
    override fun getKey() = KEY

    companion object {
        val KEY = StubIndexKey.createIndexKey<String, JaktNameIdentifierOwner>("org.serenityos.jakt.JaktNamedElementIndex")
    }
}
