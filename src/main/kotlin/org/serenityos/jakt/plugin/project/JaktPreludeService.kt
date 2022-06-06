package org.serenityos.jakt.plugin.project

import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration

interface JaktPreludeService {
    fun findPreludeType(type: String): JaktDeclaration?
}
