package org.serenityos.jakt.psi.caching

import com.intellij.ProjectTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.psi.*
import com.intellij.util.messages.Topic
import org.serenityos.jakt.JaktFile

val JAKT_STRUCTURE_CHANGE_TOPIC = Topic.create(
    "JAKT_STRUCTURE_CHANGE_TOPIC",
    JaktStructureChangeListener::class.java,
    Topic.BroadcastDirection.TO_PARENT,
)

interface JaktStructureChangeListener {
    fun onStructureChanged()
}

// This is largely taken from the intellij-rust plugin
class JaktPsiManager(val project: Project) : Disposable {
    val globalModificationTracker = JaktModificationTracker()

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(TreeChangeListener(), this)

        project.messageBus.connect(this).subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                globalModificationTracker.incModificationCount()
            }
        })
    }

    override fun dispose() { }

    fun incModificationCount(element: PsiElement) {
        element.modificationBoundary?.incModificationCount(element)
            ?: incStructureModificationCount()
    }

    inner class TreeChangeListener : JaktPsiTreeChangeListener() {
        override fun handleChange(file: PsiFile?, element: PsiElement?, parent: PsiElement?) {
            if (file == null) {
                if (element is JaktFile)
                    incStructureModificationCount()
                return
            }

            if (file !is JaktFile)
                return

            if (element is PsiComment || element is PsiWhiteSpace)
                return

            if (DumbService.isDumb(project))
                return

            parent?.let(::incModificationCount)
        }
    }

    private fun incStructureModificationCount() {
        globalModificationTracker.incModificationCount()
        project.messageBus.syncPublisher(JAKT_STRUCTURE_CHANGE_TOPIC).onStructureChanged()
    }
}
