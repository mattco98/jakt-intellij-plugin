package org.serenityos.jakt

import com.intellij.codeInsight.editorActions.moveLeftRight.MoveElementLeftRightHandler
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.psi.allChildren
import org.serenityos.jakt.psi.api.JaktScope

object JaktMoveElementLeftRightHandler : MoveElementLeftRightHandler() {
    override fun getMovableSubElements(element: PsiElement): Array<PsiElement> {
        return when (element) {
            is JaktScope -> element.getDeclarations()
            is JaktImportBraceList -> element.importBraceEntryList
            is JaktStructVisibility -> element.typeList
            is JaktNormalEnumMemberBody -> element.structEnumMemberBodyPartList
            is JaktBlock -> element.statementList
            is JaktGenericSpecialization -> element.typeList
            is JaktFunctionType -> element.parameterList?.parameterList.orEmpty()
            is JaktGenericBounds -> element.genericBoundList
            is JaktTupleType -> element.typeList
            is JaktTupleExpression -> element.expressionList
            is JaktCallExpression -> element.argumentList.argumentList
            is JaktDictionaryExpression -> element.dictionaryElementList
            is JaktSetExpression -> element.expressionList
            is JaktArrayExpression -> element.sizedArrayBody?.expressionList
                ?: element.elementsArrayBody?.expressionList
                ?: emptyList()
            is JaktMatchBody -> element.matchCaseList
            is JaktMatchCaseHead -> element.allChildren.filter {
                it.elementType == JaktTypes.ELSE_KEYWORD || it is JaktMatchPattern || it is JaktExpression
            }.toList()
            is JaktMatchPattern -> element.destructuringPartList
            is JaktLambdaCaptures -> element.lambdaCaptureList
            is JaktLogicalAndBinaryExpression -> element.expressionList
            is JaktLogicalOrBinaryExpression -> element.expressionList
            else -> emptyList()
        }.toTypedArray()
    }
}
