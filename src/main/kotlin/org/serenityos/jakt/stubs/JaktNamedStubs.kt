package org.serenityos.jakt.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.serenityos.jakt.index.JaktStructElementIndex
import org.serenityos.jakt.psi.api.*
import org.serenityos.jakt.psi.declaration.*
import org.serenityos.jakt.psi.impl.*
import org.serenityos.jakt.psi.named.JaktNamedStub
import org.serenityos.jakt.utils.BitMask
import org.serenityos.jakt.utils.isSet

class JaktStructDeclarationStub(
    parent: StubElement<*>?,
    type: IStubElementType<*, *>,
    override val name: String?,
    val flags: Int,
) : StubBase<JaktStructDeclaration>(parent, type), JaktNamedStub {
    val isExtern get() = flags.isSet(Flags.IsExtern)
    val isClass get() = flags.isSet(Flags.IsClass)

    object Type : JaktNamedStubElementType<JaktStructDeclarationStub, JaktStructDeclaration>("STRUCT_DECLARATION") {
        override fun serialize(stub: JaktStructDeclarationStub, dataStream: StubOutputStream) = with(dataStream) {
            writeName(stub.name)
            writeByte(stub.flags)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) = JaktStructDeclarationStub(
            parentStub,
            Type,
            dataStream.readNameString(),
            dataStream.readByte().toInt(),
        )

        override fun createStub(
            psi: JaktStructDeclaration,
            parentStub: StubElement<out PsiElement>?
        ) = JaktStructDeclarationStub(
            parentStub,
            Type,
            psi.name,
            BitMask.makeMask(
                Flags.IsExtern to psi.isExtern,
                Flags.IsClass to psi.isClass,
            )
        )

        override fun createPsi(stub: JaktStructDeclarationStub) = JaktStructDeclarationImpl(stub, Type)

        override fun indexStub(stub: JaktStructDeclarationStub, sink: IndexSink) {
            super.indexStub(stub, sink)
            stub.name?.let {
                sink.occurrence(JaktStructElementIndex.KEY, it)
            }
        }
    }

    enum class Flags : BitMask {
        IsExtern,
        IsClass,
    }
}

class JaktStructFieldStub(
    parent: StubElement<*>?,
    type: IStubElementType<*, *>,
    override val name: String?,
) : StubBase<JaktStructField>(parent, type), JaktNamedStub {
    object Type : JaktNamedStubElementType<JaktStructFieldStub, JaktStructField>("STRUCT_FIELD") {
        override fun serialize(stub: JaktStructFieldStub, dataStream: StubOutputStream) =
            dataStream.writeName(stub.name)

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            JaktStructFieldStub(parentStub, Type, dataStream.readNameString())

        override fun createStub(
            psi: JaktStructField,
            parentStub: StubElement<out PsiElement>?,
        ) = JaktStructFieldStub(parentStub, Type, psi.name)

        override fun createPsi(stub: JaktStructFieldStub) = JaktStructFieldImpl(stub, Type)
    }
}

class JaktEnumDeclarationStub(
    parent: StubElement<*>?,
    type: IStubElementType<*, *>,
    override val name: String?,
    val flags: Int,
) : StubBase<JaktEnumDeclaration>(parent, type), JaktNamedStub {
    val isBoxed get() = flags.isSet(Flags.IsBoxed)

    object Type : JaktNamedStubElementType<JaktEnumDeclarationStub, JaktEnumDeclaration>("ENUM_DECLARATION") {
        override fun serialize(stub: JaktEnumDeclarationStub, dataStream: StubOutputStream) = with(dataStream) {
            writeName(stub.name)
            writeByte(stub.flags)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            JaktEnumDeclarationStub(parentStub, Type, dataStream.readNameString(), dataStream.readByte().toInt())

        override fun createStub(
            psi: JaktEnumDeclaration,
            parentStub: StubElement<out PsiElement>?
        ) = JaktEnumDeclarationStub(
            parentStub,
            Type,
            psi.name,
            BitMask.makeMask(Flags.IsBoxed to psi.isBoxed),
        )

        override fun createPsi(stub: JaktEnumDeclarationStub) = JaktEnumDeclarationImpl(stub, Type)

        override fun indexStub(stub: JaktEnumDeclarationStub, sink: IndexSink) {
            super.indexStub(stub, sink)
            stub.name?.let {
                sink.occurrence(JaktStructElementIndex.KEY, it)
            }
        }
    }

    enum class Flags : BitMask {
        IsBoxed,
    }
}

class JaktNamespaceDeclarationStub(
    parent: StubElement<*>?,
    type: IStubElementType<*, *>,
    override val name: String?,
) : StubBase<JaktNamespaceDeclaration>(parent, type), JaktNamedStub {
    object Type : JaktNamedStubElementType<JaktNamespaceDeclarationStub, JaktNamespaceDeclaration>("NAMESPACE_DECLARATION") {
        override fun serialize(stub: JaktNamespaceDeclarationStub, dataStream: StubOutputStream) =
            dataStream.writeName(stub.name)

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            JaktNamespaceDeclarationStub(parentStub, Type, dataStream.readNameString())

        override fun createStub(
            psi: JaktNamespaceDeclaration,
            parentStub: StubElement<out PsiElement>?
        ) = JaktNamespaceDeclarationStub(parentStub, Type, psi.name)

        override fun createPsi(stub: JaktNamespaceDeclarationStub) = JaktNamespaceDeclarationImpl(stub, Type)
    }
}

class JaktFunctionStub(
    parent: StubElement<*>?,
    type: IStubElementType<*, *>,
    override val name: String?,
    val flags: Int,
) : StubBase<JaktFunction>(parent, type), JaktNamedStub {
    val isExtern get() = flags.isSet(Flags.IsExtern)
    val hasThis get() = flags.isSet(Flags.HasThis)
    val thisIsMutable get() = flags.isSet(Flags.ThisIsMutable)
    val throws get() = flags.isSet(Flags.Throws)

    object Type : JaktNamedStubElementType<JaktFunctionStub, JaktFunction>("FUNCTION") {
        override fun serialize(stub: JaktFunctionStub, dataStream: StubOutputStream) = with(dataStream) {
            writeName(stub.name)
            writeByte(stub.flags)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            JaktFunctionStub(parentStub, Type, dataStream.readNameString(), dataStream.readByte().toInt())

        override fun createStub(
            psi: JaktFunction,
            parentStub: StubElement<out PsiElement>?
        ) = JaktFunctionStub(
            parentStub,
            Type,
            psi.name,
            BitMask.makeMask(
                Flags.IsExtern to psi.isExtern,
                Flags.HasThis to psi.hasThis,
                Flags.ThisIsMutable to psi.thisIsMutable,
                Flags.Throws to psi.throws,
            )
        )

        override fun createPsi(stub: JaktFunctionStub) = JaktFunctionImpl(stub, Type)
    }

    enum class Flags : BitMask {
        IsExtern,
        HasThis,
        ThisIsMutable,
        Throws,
    }
}

class JaktParameterStub(
    parent: StubElement<*>?,
    type: IStubElementType<*, *>,
    override val name: String?,
) : StubBase<JaktParameter>(parent, type), JaktNamedStub {
    object Type : JaktStubElementType<JaktParameterStub, JaktParameter>("PARAMETER") {
        override fun serialize(stub: JaktParameterStub, dataStream: StubOutputStream) =
            dataStream.writeName(stub.name)

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            JaktParameterStub(parentStub, Type, dataStream.readNameString())

        override fun createStub(
            psi: JaktParameter,
            parentStub: StubElement<out PsiElement>?
        ) = JaktParameterStub(parentStub, Type, psi.name)

        override fun createPsi(stub: JaktParameterStub) = JaktParameterImpl(stub, Type)
    }
}

class JaktImportStub(
    parent: StubElement<*>?,
    type: IStubElementType<*, *>,
    override val name: String?,
    val imports: List<String>,
) : StubBase<JaktImport>(parent, type), JaktNamedStub {
    object Type : JaktStubElementType<JaktImportStub, JaktImport>("IMPORT") {
        override fun serialize(stub: JaktImportStub, dataStream: StubOutputStream) = with(dataStream) {
            writeName(stub.name)
            writeInt(stub.imports.size)
            stub.imports.forEach(::writeName)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) = with(dataStream) {
            val name = readNameString()
            val numImports = readInt()
            val imports = (0 until numImports).map { readNameString()!! }
            JaktImportStub(parentStub, Type, name, imports)
        }

        override fun createStub(
            psi: JaktImport,
            parentStub: StubElement<out PsiElement>?
        ) = JaktImportStub(parentStub, Type, psi.name, psi.importBraceList?.importBraceEntryList?.map {
            it.text
        }.orEmpty())

        override fun createPsi(stub: JaktImportStub) = JaktImportImpl(stub, Type)
    }
}

class JaktExternImportStub(
    parent: StubElement<*>?,
    type: IStubElementType<*, *>,
    val file: String,
) : StubBase<JaktExternImport>(parent, type) {
    object Type : JaktStubElementType<JaktExternImportStub, JaktExternImport>("EXTERN_IMPORT") {
        override fun serialize(stub: JaktExternImportStub, dataStream: StubOutputStream) =
            dataStream.writeUTFFast(stub.file)

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            JaktExternImportStub(parentStub, Type, dataStream.readUTFFast())

        override fun createStub(
            psi: JaktExternImport,
            parentStub: StubElement<out PsiElement>?
        ) = JaktExternImportStub(parentStub, Type, psi.stringLiteral.text)

        override fun createPsi(stub: JaktExternImportStub) = JaktExternImportImpl(stub, Type)
    }
}
