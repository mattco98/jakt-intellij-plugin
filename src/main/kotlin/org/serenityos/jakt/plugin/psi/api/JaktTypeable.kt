package org.serenityos.jakt.plugin.psi.api

import com.intellij.psi.PsiElement
import org.intellij.sdk.language.psi.JaktExpression
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.TypeInference

interface JaktTypeable : JaktPsiElement {
    val jaktType: Type
}

val JaktExpression.jaktType: Type
    get() = if (this is JaktTypeable) {
        this.jaktType
    } else {
        TypeInference.inferType(this)
    }

