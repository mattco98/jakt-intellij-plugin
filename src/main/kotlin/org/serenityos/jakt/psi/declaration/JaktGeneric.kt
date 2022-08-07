package org.serenityos.jakt.psi.declaration

import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.api.JaktGenericBound

interface JaktGeneric : PsiElement {
    fun getDeclGenericBounds(): List<JaktGenericBound>
}
