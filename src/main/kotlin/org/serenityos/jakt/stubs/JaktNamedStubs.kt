package org.serenityos.jakt.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.serenityos.jakt.index.*
import org.serenityos.jakt.psi.api.*
import org.serenityos.jakt.psi.declaration.*
import org.serenityos.jakt.psi.impl.*
import org.serenityos.jakt.psi.named.JaktNamedStub
import org.serenityos.jakt.psi.named.JaktPathedStub
import org.serenityos.jakt.utils.BitMask
import org.serenityos.jakt.utils.isSet

class JaktStructDeclarationStub(
    parent: StubElement<*>?,
    type: IStubElementType<*, *>,
    override val path: JaktPath,
    val parentPath: JaktPath?,
    val flags: Int,
) : StubBase<JaktStructDeclaration>(parent, type), JaktPathedStub {
    val isExtern get() = flags.isSet(Flags.IsExtern)
    val isClass get() = flags.isSet(Flags.IsClass)

    object Type : JaktNamedStubElementType<JaktStructDeclarationStub, JaktStructDeclaration>("STRUCT_DECLARATION") {
        override fun serialize(stub: JaktStructDeclarationStub, dataStream: StubOutputStream) = with(dataStream) {
            writePath(stub.path)
            writePath(stub.parentPath)
            writeByte(stub.flags)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) = JaktStructDeclarationStub(
            parentStub,
            Type,
            dataStream.readPath()!!,
            dataStream.readPath(),
            dataStream.readByte().toInt(),
        )

        override fun createStub(
            psi: JaktStructDeclaration,
            parentStub: StubElement<out PsiElement>?
        ) = JaktStructDeclarationStub(
            parentStub,
            Type,
            psi.toPath(),
            psi.parentPath,
            BitMask.makeMask(
                Flags.IsExtern to psi.isExtern,
                Flags.IsClass to psi.isClass,
            )
        )

        override fun createPsi(stub: JaktStructDeclarationStub) = JaktStructDeclarationImpl(stub, Type)

        override fun indexStub(stub: JaktStructDeclarationStub, sink: IndexSink) {
            super.indexStub(stub, sink)
            sink.occurrence(JaktStructElementIndex.KEY, stub.path.toString())
            stub.parentPath?.let {
                sink.occurrence(JaktStructInheritanceIndex.KEY, it.toString())
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
    override val path: JaktPath,
    val flags: Int,
) : StubBase<JaktEnumDeclaration>(parent, type), JaktPathedStub {
    val isBoxed get() = flags.isSet(Flags.IsBoxed)

    object Type : JaktNamedStubElementType<JaktEnumDeclarationStub, JaktEnumDeclaration>("ENUM_DECLARATION") {
        override fun serialize(stub: JaktEnumDeclarationStub, dataStream: StubOutputStream) = with(dataStream) {
            writePath(stub.path)
            writeByte(stub.flags)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            JaktEnumDeclarationStub(parentStub, Type, dataStream.readPath()!!, dataStream.readByte().toInt())

        override fun createStub(
            psi: JaktEnumDeclaration,
            parentStub: StubElement<out PsiElement>?
        ) = JaktEnumDeclarationStub(
            parentStub,
            Type,
            psi.toPath(),
            BitMask.makeMask(Flags.IsBoxed to psi.isBoxed),
        )

        override fun createPsi(stub: JaktEnumDeclarationStub) = JaktEnumDeclarationImpl(stub, Type)

        override fun indexStub(stub: JaktEnumDeclarationStub, sink: IndexSink) {
            super.indexStub(stub, sink)
            sink.occurrence(JaktStructElementIndex.KEY, stub.path.toString())
        }
    }

    enum class Flags : BitMask {
        IsBoxed,
    }
}

class JaktEnumVariantStub(
    parent: StubElement<*>?,
    type: IStubElementType<*, *>,
    override val name: String?,
) : StubBase<JaktEnumVariant>(parent, type), JaktNamedStub {
    object Type : JaktNamedStubElementType<JaktEnumVariantStub, JaktEnumVariant>("ENUM_VARIANT") {
        override fun serialize(stub: JaktEnumVariantStub, dataStream: StubOutputStream) =
            dataStream.writeName(stub.name)

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            JaktEnumVariantStub(parentStub, Type, dataStream.readNameString())

        override fun createStub(
            psi: JaktEnumVariant,
            parentStub: StubElement<out PsiElement>?,
        ) = JaktEnumVariantStub(parentStub, Type, psi.name)

        override fun createPsi(stub: JaktEnumVariantStub) = JaktEnumVariantImpl(stub, Type)
    }
}
class JaktCommonEnumMemberStub(
        parent: StubElement<*>?,
        type: IStubElementType<*, *>,
        override val name: String?,
) : StubBase<JaktCommonEnumMember>(parent, type), JaktNamedStub {
    object Type : JaktNamedStubElementType<JaktCommonEnumMemberStub, JaktCommonEnumMember>("COMMON_ENUM_MEMBER") {
        override fun serialize(stub: JaktCommonEnumMemberStub, dataStream: StubOutputStream) =
                dataStream.writeName(stub.name)

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                JaktCommonEnumMemberStub(parentStub, Type, dataStream.readNameString())

        override fun createStub(
                psi: JaktCommonEnumMember,
                parentStub: StubElement<out PsiElement>?,
        ) = JaktCommonEnumMemberStub(parentStub, Type, psi.name)

        override fun createPsi(stub: JaktCommonEnumMemberStub) = JaktCommonEnumMemberImpl(stub, Type)
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
    val isVirtual get() = flags.isSet(Flags.IsVirtual)
    val isOverride get() = flags.isSet(Flags.IsOverride)

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
                Flags.IsVirtual to psi.isVirtual,
                Flags.IsOverride to psi.isOverride,
            )
        )

        override fun createPsi(stub: JaktFunctionStub) = JaktFunctionImpl(stub, Type)
    }

    enum class Flags : BitMask {
        IsExtern,
        HasThis,
        ThisIsMutable,
        Throws,
        IsVirtual,
        IsOverride,
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
