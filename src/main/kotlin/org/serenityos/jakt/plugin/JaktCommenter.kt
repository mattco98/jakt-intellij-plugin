package org.serenityos.jakt.plugin

import com.intellij.lang.Commenter

class JaktCommenter : Commenter {
    override fun getLineCommentPrefix() = "//"

    override fun getBlockCommentPrefix() = null

    override fun getBlockCommentSuffix() = null

    override fun getCommentedBlockCommentPrefix() = null

    override fun getCommentedBlockCommentSuffix() = null
}
