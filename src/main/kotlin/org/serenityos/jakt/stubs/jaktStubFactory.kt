package org.serenityos.jakt.stubs

fun jaktStubFactory(name: String): JaktStubElementType<*, *> = when (name) {
    "STRUCT_DECLARATION" -> JaktStructDeclarationStub.Type
    "ENUM_DECLARATION" -> JaktEnumDeclarationStub.Type
    "NAMESPACE_DECLARATION" -> JaktNamespaceDeclarationStub.Type
    "FUNCTION" -> JaktFunctionStub.Type
    "IMPORT" -> JaktImportStub.Type
    "EXTERN_IMPORT" -> JaktExternImportStub.Type
    "PARAMETER" -> JaktParameterStub.Type
    else -> error("unknown stub element $name")
}
