package org.serenityos.jakt.clion

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement

object JaktCppHostInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val langs = Language.getRegisteredLanguages()
        registrar.startInjecting(TODO())
    }

    override fun elementsToInjectIn(): MutableList<out Class<out PsiElement>> {
        TODO("Not yet implemented")
    }
}
