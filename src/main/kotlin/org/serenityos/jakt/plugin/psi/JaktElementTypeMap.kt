package org.serenityos.jakt.plugin.psi

import org.serenityos.jakt.bindings.JaktCheckedType
import org.serenityos.jakt.bindings.JaktProject

class JaktElementTypeMap(val project: JaktProject) {
    val elementToTypeMap = mutableMapOf<JaktPsiElement, JaktCheckedType>()
    val typeToElementMap = mutableMapOf<JaktCheckedType, JaktPsiElement>()

    fun insert(element: JaktPsiElement, type: JaktCheckedType) {
        check(element !in elementToTypeMap) {
            "Element $element is already in the ElementTypeMap"
        }
        check(type !in typeToElementMap) {
            "Type $type is already in the ElementTypeMap"
        }

        elementToTypeMap[element] = type
        typeToElementMap[type] = element
    }

    inline fun <reified T : JaktPsiElement> getAssociatedElement(type: JaktCheckedType) = typeToElementMap[type].let {
        requireNotNull(it) {
            "Type $type has no entry in the ElementTypeMap"
        }
        require(it is T) {
            "Expected $type's associated element to be a ${T::class.simpleName}, but found ${it::class.simpleName}"
        }
        it
    }

    inline fun <reified T : JaktCheckedType> getAssociatedType(element: JaktPsiElement) = elementToTypeMap[element].let {
        requireNotNull(it) {
            "Element $element has no entry in the ElementTypeMap"
        }
        require(it is T) {
            "Expected $element's associated type to be a ${T::class.simpleName}, but found ${it::class.simpleName}"
        }
        it
    }
}
