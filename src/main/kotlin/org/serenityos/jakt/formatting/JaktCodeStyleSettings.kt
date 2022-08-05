package org.serenityos.jakt.formatting

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings

@Suppress("PropertyName")
class JaktCodeStyleSettings(
    container: CodeStyleSettings,
) : CustomCodeStyleSettings("JaktCodeStyleSettings", container) {
    @JvmField
    var SPACE_IN_SET_DELIMS: Boolean = false

    @JvmField
    var SPACE_IN_ARRAY_DELIMS: Boolean = false

    @JvmField
    var SPACE_IN_DICTIONARY_DELIMS: Boolean = false

    @JvmField
    var SPACE_IN_TUPLE_DELIMS: Boolean = false

    @JvmField
    var SPACE_BETWEEN_EMPTY_BRACES: Boolean = false
}
