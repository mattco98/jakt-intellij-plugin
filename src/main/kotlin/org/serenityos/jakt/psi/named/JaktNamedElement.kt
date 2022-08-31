package org.serenityos.jakt.psi.named

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.index.JaktPath
import org.serenityos.jakt.psi.JaktPsiFactory
import org.serenityos.jakt.render.getIcon
import org.serenityos.jakt.render.getPresentation

abstract class JaktNamedElement(node: ASTNode) : ASTWrapperPsiElement(node), JaktNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement? = findChildByType(JaktTypes.IDENTIFIER)

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier?.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getTextOffset() = nameIdentifier?.textOffset ?: super.getTextOffset()
}

interface JaktNamedStub {
    val name: String?
}

interface JaktPathedStub : JaktNamedStub {
    val path: JaktPath

    override val name: String?
        get() = path.name
}

abstract class JaktStubbedNamedElement<T> : StubBasedPsiElementBase<T>, JaktNameIdentifierOwner
    where T : StubElement<*>, T : JaktNamedStub
{
    constructor(node: ASTNode) : super(node)

    constructor(stub: T, type: IStubElementType<*, *>) : super(stub, type)

    override fun getNameIdentifier(): PsiElement? = findChildByType(JaktTypes.IDENTIFIER)

    override fun getName(): String = greenStub?.name ?: nameIdentifier?.text.orEmpty()

    override fun setName(name: String): PsiElement? = apply {
        nameIdentifier?.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getTextOffset() = nameIdentifier?.textOffset ?: super.getTextOffset()

    override fun getPresentation(): ItemPresentation? = getPresentation(this)

    override fun getIcon(flags: Int) = getIcon(this)
}
