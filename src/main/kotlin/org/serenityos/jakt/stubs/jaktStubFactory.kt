package org.serenityos.jakt.stubs

fun jaktStubFactory(name: String): JaktStubElementType<*, *> = when (name) {
    "STRUCT_DECLARATION" -> JaktStructDeclarationStub.Type
    else -> error("unknown stub element $name")
}
