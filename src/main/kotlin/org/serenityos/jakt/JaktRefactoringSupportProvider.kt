package org.serenityos.jakt

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.named.JaktNameIdentifierOwner

class JaktRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        return element is JaktNameIdentifierOwner
    }
}
