package org.serenityos.jakt.plugin.completions

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.openapi.util.Key
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.serenityos.jakt.plugin.psi.JaktPsiElement

abstract class JaktCompletion : CompletionProvider<CompletionParameters>() {
    abstract val pattern: ElementPattern<PsiElement>
}

inline fun <reified T : JaktPsiElement> psiElement() = PlatformPatterns.psiElement(T::class.java)

inline fun <reified T : JaktPsiElement> condition(
    debugName: String,
    crossinline block: (element: T, context: ProcessingContext?) -> Boolean,
) = object : PatternCondition<T>(debugName) {
    override fun accepts(t: T, context: ProcessingContext?) = block(t, context)
}

inline operator fun <reified T> ProcessingContext.set(key: Key<T>, value: T) = this.put(key, value)
