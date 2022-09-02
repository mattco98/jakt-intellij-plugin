package org.serenityos.jakt.psi

import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.declaration.JaktDeclaration

interface JaktADT : JaktDeclaration {
    fun canHaveMethods(): Boolean = true

    fun getMethods(): List<JaktFunction>

    fun getSuperElement(): JaktADT? = null

    fun getBodyStartAnchor(): PsiElement?
}
