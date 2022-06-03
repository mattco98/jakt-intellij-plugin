package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.psi.PsiNameIdentifierOwner
import org.serenityos.jakt.plugin.psi.api.JaktTypeable

interface JaktDeclaration : JaktTypeable, JaktNameIdentifierOwner
