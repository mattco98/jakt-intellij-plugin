package org.serenityos.jakt.plugin.psi.api

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import org.serenityos.jakt.plugin.psi.JaktPsiElement

interface JaktModificationBoundary : JaktPsiElement {
    val tracker: ModificationTracker
}

val JaktPsiElement.modificationBoundary: JaktModificationBoundary
    get() {
        var element: PsiElement = this
        while (element !is JaktModificationBoundary)
            element = element.parent
        return element
    }
