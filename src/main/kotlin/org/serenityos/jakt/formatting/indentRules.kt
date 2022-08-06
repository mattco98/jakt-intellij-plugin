package org.serenityos.jakt.formatting

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.utils.BLOCK_LIKE
import org.serenityos.jakt.utils.LIST_LIKE
import org.serenityos.jakt.utils.isDelimiterFor

fun findIndentForNode(node: ASTNode): Indent? {
    val parent = node.treeParent ?: return Indent.getNoneIndent()
    val parentType = parent.elementType

    if (parent.psi is JaktFile || node.isDelimiterFor(parent))
        return Indent.getNoneIndent()

    if (parentType in BLOCK_LIKE || parentType in LIST_LIKE)
        return Indent.getNormalIndent()

    return Indent.getNoneIndent()
}
