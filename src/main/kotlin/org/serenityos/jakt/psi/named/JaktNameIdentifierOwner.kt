package org.serenityos.jakt.psi.named

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.serenityos.jakt.psi.JaktPsiElement

interface JaktNameIdentifierOwner : JaktPsiElement, PsiNameIdentifierOwner, NavigatablePsiElement
