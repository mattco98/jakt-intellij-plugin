package org.serenityos.jakt.plugin.completions

import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateEditingListener
import com.intellij.codeInsight.template.impl.TemplateState

// Simple class with empty overrides so inheritors can override only the
// methods they are interested in
abstract class JaktTemplateEditingListener : TemplateEditingListener {
    override fun beforeTemplateFinished(state: TemplateState, template: Template?) {
    }

    override fun templateFinished(template: Template, brokenOff: Boolean) {
    }

    override fun templateCancelled(template: Template?) {
    }

    override fun currentVariableChanged(
        templateState: TemplateState,
        template: Template?,
        oldIndex: Int,
        newIndex: Int
    ) {
    }

    override fun waitingForInput(template: Template?) {
    }
}