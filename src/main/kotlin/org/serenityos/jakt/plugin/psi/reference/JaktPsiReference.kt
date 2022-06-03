package org.serenityos.jakt.plugin.psi.reference

import com.intellij.psi.PsiPolyVariantReferenceBase
import org.serenityos.jakt.plugin.psi.declaration.JaktNameIdentifierOwner

abstract class JaktPsiReference(element: JaktNameIdentifierOwner) :
    PsiPolyVariantReferenceBase<JaktNameIdentifierOwner>(element)
