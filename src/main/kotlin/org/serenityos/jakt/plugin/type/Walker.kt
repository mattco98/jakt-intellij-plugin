package org.serenityos.jakt.plugin.type

import com.intellij.psi.PsiElement
import org.serenityos.jakt.utils.allChildren

enum class WalkResult {
    Continue,
    SkipSiblings,
    Cancel,
}

fun walkPsiElement(element: PsiElement, block: (PsiElement) -> WalkResult): Boolean {
    for (child in element.allChildren) {
        when (block(child)) {
            WalkResult.Continue -> {}
            WalkResult.SkipSiblings -> return true
            WalkResult.Cancel -> return false
        }

        if (!walkPsiElement(child, block))
            return false
    }

    return true
}
