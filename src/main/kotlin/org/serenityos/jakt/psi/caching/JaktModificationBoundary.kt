package org.serenityos.jakt.psi.caching

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.ancestorOfType
import java.util.concurrent.atomic.AtomicLong

class JaktModificationTracker : ModificationTracker {
    private val counter = AtomicLong(0)

    override fun getModificationCount() = counter.get()

    fun incModificationCount() {
        counter.getAndIncrement()
    }
}

interface JaktModificationBoundary : PsiElement {
    val tracker: JaktModificationTracker

    fun incModificationCount(from: PsiElement) = tracker.incModificationCount()
}

val PsiElement.modificationBoundary: JaktModificationBoundary?
    get() = ancestorOfType<JaktModificationBoundary>()
