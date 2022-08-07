package org.serenityos.jakt.structure

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.TreeAnchorizer
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.RowIcon
import com.intellij.util.PlatformIcons
import com.intellij.util.ReflectionUtil
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.JaktScope
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.*
import org.serenityos.jakt.psi.findChildOfType
import org.serenityos.jakt.render.renderElement
import org.serenityos.jakt.utils.unreachable
import javax.swing.Icon

class JaktStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile) = Builder(psiFile)

    class Builder(private val file: PsiFile) : TreeBasedStructureViewBuilder() {
        override fun createStructureViewModel(editor: Editor?) = Model(file, editor)
    }

    class Model(
        file: PsiFile,
        editor: Editor?,
    ) : StructureViewModelBase(file, editor, Element(file)), StructureViewModel.ElementInfoProvider {
        init {
            withSuitableClasses(*suitableClasses)
        }

        override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean {
            return element.value is JaktFile
        }

        override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean {
            return element is JaktFunction
        }

        companion object {
            private val suitableClasses = arrayOf(
                JaktFile::class.java,
                JaktFunction::class.java,
                JaktStructDeclaration::class.java,
                JaktStructField::class.java,
                JaktEnumDeclaration::class.java,
                JaktEnumVariant::class.java,
            )

            fun isSuitable(element: PsiElement) = suitableClasses.any {
                ReflectionUtil.isAssignable(it, element::class.java)
            }
        }
    }

    class Element(element: PsiElement) : StructureViewTreeElement {
        private val psiAnchor = TreeAnchorizer.getService().createAnchor(element)
        private val psi: PsiElement? get() = TreeAnchorizer.getService().retrieveElement(psiAnchor) as? PsiElement

        override fun getPresentation(): ItemPresentation {
            return psi?.let {
                val text = when (it) {
                    is JaktEnumVariant -> it.name
                    is JaktFile -> it.name
                    else -> renderElement(it) { showStructure = true }
                }

                PresentationData(text, null, getIcon(), null)
            } ?: PresentationData("", null, null, null)
        }

        override fun getChildren(): Array<TreeElement> {
            return when (val psi = psi) {
                is JaktFunction -> emptyArray()
                is JaktScope -> psi.getDeclarations().filter(Model::isSuitable).map(::Element).toTypedArray()
                else -> emptyArray()
            }
        }

        override fun navigate(requestFocus: Boolean) {
            (psi as? Navigatable)?.navigate(requestFocus)
        }

        override fun canNavigate(): Boolean = (psi as? Navigatable)?.canNavigate() == true

        override fun canNavigateToSource(): Boolean = (psi as? Navigatable)?.canNavigateToSource() == true

        override fun getValue() = psi

        private fun getIcon(): Icon? {
            val psi = psi ?: return null
            val baseIcon = getBaseIcon(psi)
            return psi.findChildOfType<JaktStructVisibility>()?.let {
                RowIcon(baseIcon, getVisibilityIcon(it))
            } ?: baseIcon
        }

        private fun getBaseIcon(element: PsiElement) = when (element) {
            is JaktStructDeclaration -> PlatformIcons.CLASS_ICON
            is JaktEnumDeclaration -> PlatformIcons.ENUM_ICON
            is JaktEnumVariant -> PlatformIcons.ENUM_ICON
            is JaktFunction -> if (element.ancestorOfType<JaktStructDeclaration>() != null) {
                PlatformIcons.METHOD_ICON
            } else PlatformIcons.FUNCTION_ICON
            is JaktStructField -> PlatformIcons.FIELD_ICON
            else -> null
        }

        private fun getVisibilityIcon(visibility: JaktStructVisibility) = when {
            visibility.publicKeyword != null -> PlatformIcons.PUBLIC_ICON
            visibility.privateKeyword != null -> PlatformIcons.PRIVATE_ICON
            visibility.restrictedKeyword != null -> PlatformIcons.PROTECTED_ICON
            else -> unreachable()
        }
    }
}
