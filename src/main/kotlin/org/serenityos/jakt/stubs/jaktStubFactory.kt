package org.serenityos.jakt.stubs


fun jaktStubFactory(name: String): JaktStubElementType<*, *> = when (name) {
    "STRUCT_DECLARATION" -> JaktStructDeclarationStub.Type
    "STRUCT_FIELD" -> JaktStructFieldStub.Type
    "ENUM_DECLARATION" -> JaktEnumDeclarationStub.Type
    "ENUM_VARIANT" -> JaktEnumVariantStub.Type
    "COMMON_ENUM_MEMBER" -> JaktCommonEnumMemberStub.Type
    "NAMESPACE_DECLARATION" -> JaktNamespaceDeclarationStub.Type
    "FUNCTION" -> JaktFunctionStub.Type
    "IMPORT" -> JaktImportStub.Type
    "EXTERN_IMPORT" -> JaktExternImportStub.Type
    "PARAMETER" -> JaktParameterStub.Type
    else -> error("unknown stub element $name")
}
