package org.serenityos.jakt.codeInsight

import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.MemberChooserObjectBase
import com.intellij.codeInsight.hint.HintManager
import com.intellij.icons.AllIcons
import com.intellij.ide.util.MemberChooser
import com.intellij.lang.LanguageCodeInsightActionHandler
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.serenityos.jakt.psi.JaktADT
import org.serenityos.jakt.psi.JaktPsiFactory
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.declaration.isVirtual
import javax.swing.Icon

class JaktOverrideMethodsHandler : LanguageCodeInsightActionHandler {
    override fun isValidFor(editor: Editor, file: PsiFile): Boolean {
        val element = file.findElementAt(editor.caretModel.offset) ?: return false
        return element.ancestorOfType<JaktADT>()?.canHaveMethods() ?: false
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val element = file.findElementAt(editor.caretModel.offset) ?: return
        val structOrEnum = element.ancestorOfType<JaktADT>() ?: return
        if (!structOrEnum.canHaveMethods())
            return

        val allSuperMethods = generateSequence(structOrEnum.getSuperElement()) {
            it.getSuperElement()
        }.flatMap {
            it.getMethods()
        }.distinctBy { it.name }.filter {
            it.isVirtual
        }

        val allSelfMethods = structOrEnum.getMethods()
        val allSelfMethodNames = allSelfMethods.map { it.name }.toSet()

        val methodsThatCanBeOverridden = allSuperMethods.filter {
            it.name !in allSelfMethodNames
        }.toList()

        if (methodsThatCanBeOverridden.isEmpty()) {
            HintManager.getInstance().showErrorHint(editor, "No methods to override have been found")
            return
        }

        val parentChooser = MemberChooserObjectBase(structOrEnum.name ?: return, AllIcons.Nodes.Class)

        val members = methodsThatCanBeOverridden.mapNotNull {
            JaktClassMember(
                it.name ?: return@mapNotNull null,
                AllIcons.Nodes.Method,
                it,
                parentChooser
            )
        }

        // TODO: When Jakt gets abstract methods, they should be selected by default
        //       with chooser.selectElements()
        val chooser = MemberChooser(members.toTypedArray(), true, true, project)
        chooser.title = "Implement Methods"
        chooser.show()

        val factory = JaktPsiFactory(project)

        val methodsToOverride = chooser.selectedElements?.map {
            val parentMethod = it.method

            factory.createFunction(buildString {
                append("virtual override function ")
                append(parentMethod.name)
                append(parentMethod.parameterList.text)

                if (parentMethod.fatArrow != null) {
                    append(" => abort() // TODO")
                } else {
                    append("{\n        abort() // TODO\n    }")
                }
            })
        } ?: return

        // Insert methods near the cursor, if possible
        val previousMethod = (allSelfMethods.indexOfFirst {
            it.textRange.startOffset > editor.caretModel.offset
        } - 1).let {
            if (it >= 0) allSelfMethods[it] else null
        }

        runWriteAction {
            val anchor = previousMethod ?: structOrEnum.getBodyStartAnchor() ?: return@runWriteAction
            val parent = anchor.parent ?: return@runWriteAction

            for ((index, method) in methodsToOverride.asReversed().withIndex()) {
                parent.addAfter(method, anchor)
                parent.addAfter(factory.newline, anchor)
                if (index != methodsToOverride.lastIndex)
                    parent.addAfter(factory.newline, anchor)
            }
        }
    }
}

class JaktClassMember(
    name: String,
    icon: Icon,
    val method: JaktFunction,
    private val parent: MemberChooserObject,
) : MemberChooserObjectBase(name, icon), ClassMember {
    override fun getParentNodeDelegate() = parent
}
