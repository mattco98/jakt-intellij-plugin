package org.serenityos.jakt.render

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiNamedElement
import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.api.*
import javax.swing.Icon

fun getPresentation(psi: JaktPsiElement): ItemPresentation {
    val baseDirectory = psi.project.guessProjectDir()?.toNioPath()?.toFile()
    val psiFile = psi.containingFile?.virtualFile?.toNioPath()?.toFile()
    val location = if (baseDirectory != null && psiFile != null) {
        psiFile.toRelativeString(baseDirectory)
    } else psiFile?.absolutePath

    val name = (psi as? PsiNamedElement)?.name

    return PresentationData(name, location, getIcon(psi), null)
}

// TODO: Better icons, maybe custom?
fun getIcon(psi: JaktPsiElement): Icon? = when (psi) {
    is JaktStructDeclaration -> AllIcons.Nodes.Class
    is JaktStructField -> AllIcons.Nodes.Field
    is JaktFunction -> AllIcons.Nodes.Function
    is JaktNamespaceDeclaration -> AllIcons.Nodes.Module
    is JaktImport -> AllIcons.Nodes.Include
    is JaktEnumDeclaration -> AllIcons.Nodes.Enum
    is JaktParameter -> AllIcons.Nodes.Parameter
    else -> null
}
