package org.serenityos.jakt.formatting

import com.intellij.formatting.Alignment
import com.intellij.lang.ASTNode
import org.serenityos.jakt.utils.BLOCK_LIKE
import org.serenityos.jakt.utils.LIST_LIKE

fun findAlignmentForNode(node: ASTNode): Alignment? {
    val parentType = node.treeParent?.elementType ?: return null

    if (parentType in BLOCK_LIKE || parentType in LIST_LIKE)
        return Alignment.createAlignment()

    return null
}
