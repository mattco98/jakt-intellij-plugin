package org.serenityos.jakt.psi.api

import com.intellij.openapi.util.ModificationTracker
import org.serenityos.jakt.psi.JaktPsiElement

interface JaktModificationBoundary : JaktPsiElement {
    val tracker: ModificationTracker
}

class JaktModificationTracker : ModificationTracker {
    override fun getModificationCount(): Long {
        // TODO: Actually implement invalidation logic
        return ModificationTracker.EVER_CHANGED.modificationCount
    }
}
