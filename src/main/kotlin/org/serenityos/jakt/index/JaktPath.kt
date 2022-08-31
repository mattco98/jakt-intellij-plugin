package org.serenityos.jakt.index

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import org.serenityos.jakt.psi.declaration.nameNonNull

// Represents a named element in a particular file and namespace
data class JaktPath(
    val namespaces: List<String>,
    val name: String,
) {
    override fun toString() = namespaces.joinToString(separator = "/") + "/$name"
}

fun JaktDeclaration.toPath() = JaktPath(
    generateSequence(this) { it.ancestorOfType<JaktDeclaration>() }.drop(1).map { it.nameNonNull }.toList(),
    nameNonNull,
)

fun StubOutputStream.writePath(path: JaktPath?) {
    // Nullability boolean
    writeBoolean(path != null)

    if (path != null) {
        require(path.namespaces.size < Byte.MAX_VALUE)
        writeByte(path.namespaces.size)
        path.namespaces.forEach(::writeName)
        writeName(path.name)
    }
}

fun StubInputStream.readPath(): JaktPath? {
    return if (readBoolean()) {
        JaktPath(
            (0 until readByte()).map { readNameString()!! },
            readNameString()!!
        )
    } else null
}
