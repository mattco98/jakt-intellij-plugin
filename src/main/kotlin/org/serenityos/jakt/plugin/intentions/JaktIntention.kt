package org.serenityos.jakt.plugin.intentions

import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

abstract class JaktIntention<T>(private val description: String) : BaseElementAtCaretIntentionAction() {
    private var cachedContext: T? = null

    // TODO: What is the difference between these methods?
    override fun getText() = description
    override fun getFamilyName() = description

    abstract fun getContext(project: Project, editor: Editor, element: PsiElement): T?

    abstract fun apply(project: Project, editor: Editor, context: T)

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        cachedContext = getContext(project, editor, element)
        return cachedContext != null
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        apply(project, editor, cachedContext!!)
        cachedContext = null
    }
}
