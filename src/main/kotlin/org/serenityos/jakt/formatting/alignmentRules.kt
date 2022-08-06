package org.serenityos.jakt.formatting

import com.intellij.formatting.Alignment
import com.intellij.lang.ASTNode
import org.serenityos.jakt.JaktTypes.IMPORT_BRACE_LIST
import org.serenityos.jakt.utils.WHITE_SPACE
import org.serenityos.jakt.utils.isDelimiterFor

private val IMPORT_ALIGNMENT = Alignment.createAlignment()

fun findAlignmentForNode(node: ASTNode): Alignment? {
    val parent = node.treeParent
    val parentType = node.treeParent?.elementType ?: return null

    if (node.isDelimiterFor(parent) || node.elementType in WHITE_SPACE)
        return null

    if (parentType == IMPORT_BRACE_LIST)
        return IMPORT_ALIGNMENT

    return null
}
