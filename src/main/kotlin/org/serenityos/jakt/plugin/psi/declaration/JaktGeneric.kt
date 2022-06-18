package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktGenericBound

interface JaktGeneric : PsiElement {
    fun getDeclGenericBounds(): List<JaktGenericBound>
}
