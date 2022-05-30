package org.serenityos.jakt.plugin.annotations

import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.bindings.*
import org.serenityos.jakt.plugin.JaktFile
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.declarations.JaktDeclaration
import org.serenityos.jakt.plugin.psi.declarations.JaktParameterImplMixin
import org.serenityos.jakt.utils.*

class JaktTypeAnalyzer(private val file: PsiFile, private val jaktProject: JaktProject) {
    private val elementTypeMap = (file as JaktFile).elementTypeMap

    private val scopeStack = mutableListOf<ScopeId>()
    private val currentScope: ScopeId
        get() = scopeStack.last()

    fun walk() {
        require(file is JaktFile)
        withScope(0) {
            walkScope(file)
        }
    }

    private fun walkScope(element: JaktPsiElement) {
        for (child in element.children) {
            when (child) {
                is JaktStructDeclaration -> {
                    val structId = jaktProject.findStructInScope(currentScope, child.identifier.text)!!
                    val struct = jaktProject.structs[structId]
                    walkStruct(child, struct)
                }
                is JaktFunctionDeclaration -> {
                    val functionId = jaktProject.findFunctionInScope(currentScope, child.identifier.text)!!
                    val function = jaktProject.functions[functionId]
                    walkFunction(child, function)
                }
                is JaktEnumDeclaration -> {
                    val enumId = jaktProject.findEnumInScope(currentScope, child.identifier.text)!!
                    val enum = jaktProject.enums[enumId]
                    walkEnum(child, enum)
                }
                is JaktExternFunctionDeclaration -> {
                    // TODO
                }
                is JaktExternStructDeclaration -> {
                    // TODO
                }
                is JaktNamespaceDeclaration -> {
                    val namespaceId = jaktProject.findNamespaceInScope(currentScope, child.identifier.text)!!
                    withScope(namespaceId) {
                        walkScope(child)
                    }
                }
                else -> {
                    if (child.elementType != JaktTypes.NEWLINE)
                        error("Unexpected top level declaration $child")
                }
            }
        }
    }

    private inline fun <reified T : JaktCheckedType> JaktCheckedType.cast(): T = this as? T
        ?: error("Failed to cast ${this::class.simpleName} to ${T::class.simpleName}")

    private fun walkStruct(element: JaktStructDeclaration, checkedType: CheckedStruct) {
        element.genericBounds?.also { walkGenericBounds(it, checkedType.genericParameters) }

        val scope = jaktProject.scopes[checkedType.scopeId]
        val methods = scope.functions.associate { (name, id, _) ->
            name to jaktProject.functions[id]
        }

        withScope(checkedType.scopeId) {
            element.structBody.structMemberList.forEach {
                val function = it.functionDeclaration
                if (function != null) {
                    walkFunction(function, methods[function.identifier.text]!!)
                } else {
                    val field = it.structField
                    TODO()
                }
            }
        }
    }

    private fun walkFunction(element: JaktFunctionDeclaration, checkedType: CheckedFunction) {
        element.genericBounds?.also {
            walkGenericBounds(it, checkedType.genericParameters.map(FunctionGenericParameter::typeId))
        }

        withScope(checkedType.functionScopeId) {
            (element.parameterList zipSafe checkedType.parameters).forEach { (param, type) ->
                walkParameter(param, type)
                (param as JaktParameterImplMixin).owningFunction = element
            }

            element.functionReturnType?.also { walkType(it.type, checkedType.returnTypeId) }

            // walkBlock uses withScope, so omit it here
            if (element.block != null) {
                walkBlock(element.block!!, checkedType.block)
            } else {
                // Rewrite the expression statement into a block
                val factory = JaktPsiFactory(element.project)
                val expr = element.findNotNullChildOfType<JaktExpression>()
                val block = factory.createBlock(factory.createReturnStatement(expr))
                walkBlock(block, checkedType.block)
            }
        }
    }

    private fun walkParameter(element: JaktParameter, checkedType: CheckedParameter) {
        elementTypeMap.insert(element, checkedType.variable)
        element.declarationReferences = mutableListOf()

        walkType(element.typeAnnotation!!.type, checkedType.variable.typeId)
    }

    private fun walkGenericBounds(element: JaktGenericBounds, checkedTypes: List<TypeId>) {
        (element.typeList zipSafe checkedTypes).forEach { (type, id) -> walkType(type, id) }
    }

    private fun walkType(element: JaktType, typeId: TypeId) {
    }

    private fun walkEnum(element: JaktEnumDeclaration, checkedType: CheckedEnum) {
        // TODO
        // val normalBody = element.normalEnumBody
        // if (normalBody != null) {
        //
        // } else {
        //     val underlyingBody = element.underlyingTypeEnumBody!!
        //     walkType(underlyingBody.typeAnnotation.type, checkedType.underlyingTypeId!!)
        //     underlyingBody.underlyingTypeEnumMemberList zipSafe checkedType.
        // }
    }

    private fun walkStatement(element: JaktStatement, checkedType: CheckedStatement) {
        // Switch on the type, since it is a sealed class, and will emit a warning if
        // other statements are added to the type tree but not checked here.
        when (checkedType) {
            is CheckedStatement.Block -> {
                require(element is JaktBlock)
                walkBlock(element, checkedType.body)
            }
            is CheckedStatement.Break -> {}
            is CheckedStatement.Continue -> {}
            is CheckedStatement.Defer -> {
                require(element is JaktDeferStatement)
                walkDefer(element, checkedType)
            }
            is CheckedStatement.Expression -> {
                walkExpression(element.expression!!, checkedType.expression)
            }
            is CheckedStatement.Garbage -> {}
            is CheckedStatement.If -> {
                require(element is JaktIfStatement)
                walkIf(element, checkedType)
            }
            is CheckedStatement.InlineCpp -> {
                require(element is JaktInlineCppStatement)
                walkInlineCpp(element, checkedType)
            }
            is CheckedStatement.Loop -> {
                require(element is JaktLoopStatement)
                walkLoop(element, checkedType)
            }
            is CheckedStatement.Return -> {
                require(element is JaktReturnStatement)
                walkReturn(element, checkedType)
            }
            is CheckedStatement.Throw -> {
                require(element is JaktThrowStatement)
                walkThrow(element, checkedType)
            }
            is CheckedStatement.Try -> {
                TODO("Add try statements to grammar")
                // require(element is JaktTryStatement)
                // walkTry(element, checkedType)
            }
            is CheckedStatement.VarDecl -> {
                require(element is JaktVariableDeclarationStatement)
                walkVarDecl(element, checkedType)
            }
            is CheckedStatement.While -> {
                require(element is JaktWhileStatement)
                walkWhile(element, checkedType)
            }
        }
    }

    private fun walkBlock(element: JaktBlock, checkedType: CheckedBlock) {
        val statements = element.findChildrenOfType<JaktStatement>()
        withScope(checkedType.scopeId) {
            (statements zipSafe checkedType.statements).forEach { (statement, type) ->
                walkStatement(statement, type)
            }
        }
    }

    private fun walkDefer(element: JaktDeferStatement, checkedType: CheckedStatement.Defer) {
        walkStatement(element.statement, checkedType.statement)
    }

    private fun walkIf(element: JaktIfStatement, checkedType: CheckedStatement.If) {
        walkExpression(element.findNotNullChildOfType(), checkedType.condition)
        walkBlock(element.blockList[0], checkedType.ifBlock)
        if (element.blockList.size > 1)
            walkBlock(element.blockList[1], checkedType.elseBlock!!)
    }

    private fun walkInlineCpp(element: JaktInlineCppStatement, checkedType: CheckedStatement.InlineCpp) {
    }

    private fun walkLoop(element: JaktLoopStatement, checkedType: CheckedStatement.Loop) {
        walkBlock(element.block, checkedType.body)
    }

    private fun walkReturn(element: JaktReturnStatement, checkedType: CheckedStatement.Return) {
        element.expression?.also { walkExpression(it, checkedType.value) }
    }

    private fun walkThrow(element: JaktThrowStatement, checkedType: CheckedStatement.Throw) {
        walkExpression(element.findNotNullChildOfType(), checkedType.target)
    }

    private fun walkVarDecl(element: JaktVariableDeclarationStatement, checkedType: CheckedStatement.VarDecl) {
        val variable = jaktProject.findVarInScope(currentScope, checkedType.varDecl.name)!!
        elementTypeMap.insert(element, variable)
        element.declarationReferences = mutableListOf()

        element.typeAnnotation?.type?.also { walkType(it, checkedType.varDecl.typeId) }
        walkExpression(element.findChildrenOfType<JaktExpression>()[1], checkedType.initializer)
    }

    private fun walkWhile(element: JaktWhileStatement, checkedType: CheckedStatement.While) {
        walkExpression(element.expression!!, checkedType.condition)
        walkBlock(element.block, checkedType.body)
    }

    private fun walkExpression(element: JaktExpression, checkedType: CheckedExpression) {
        // Switch on the type, since it is a sealed class, and will emit a warning if
        // other expressions are added to the type tree but not checked here.
        when (checkedType) {
            is CheckedExpression.Array -> {
                require(element is JaktArrayExpression)
                walkArray(element, checkedType.cast())
            }
            is CheckedExpression.BinaryOp -> {
                require(element is JaktBinaryExpression)
                walkBinaryOp(element, checkedType.cast())
            }
            is CheckedExpression.Boolean,
            is CheckedExpression.ByteConstant,
            is CheckedExpression.CharacterConstant,
            is CheckedExpression.NumericConstant,
            is CheckedExpression.QuotedString -> {}
            is CheckedExpression.Call -> {
                require(element is JaktCallExpression)
                walkCall(element, checkedType.cast())
            }
            is CheckedExpression.Dictionary -> {
                require(element is JaktDictionaryExpression)
                walkDictionary(element, checkedType.cast())
            }
            is CheckedExpression.ForcedUnwrap -> {
                // TODO: Add to grammar
                // require(element is JaktForcedUnwrapExpression)
                // walkForcedUnwrap(element, checkedType.cast())
            }
            is CheckedExpression.Garbage -> {}
            is CheckedExpression.IndexedDictionary -> {
                require(element is JaktIndexedAccessExpression)
                walkIndexedDictionary(element, checkedType.cast())
            }
            is CheckedExpression.IndexedExpression -> {
                require(element is JaktIndexedAccessExpression)
                walkIndexedExpression(element, checkedType.cast())
            }
            is CheckedExpression.IndexedStruct -> {
                require(element is JaktIndexedStructExpression)
                walkIndexedStruct(element, checkedType.cast())
            }
            is CheckedExpression.IndexedTuple -> {
                require(element is JaktIndexedTupleExpression)
                walkIndexedTuple(element, checkedType.cast())
            }
            is CheckedExpression.Match -> {
                require(element is JaktMatchExpression)
                walkMatch(element, checkedType.cast())
            }
            is CheckedExpression.MethodCall -> {
                require(element is JaktCallExpression)
                walkMethodCall(element, checkedType.cast())
            }
            is CheckedExpression.NamespacedVar -> {
                // TODO
                // require(element is JaktNamespacedVarExpression)
                // walkNamespacedVar(element, checkedType.cast())
            }
            is CheckedExpression.OptionalNone -> {
                require(element is JaktOptionalNoneExpression)
                walkOptionalNone(element, checkedType.cast())
            }
            is CheckedExpression.OptionalSome -> {
                require(element is JaktOptionalSomeExpression)
                walkOptionalSome(element, checkedType.cast())
            }
            is CheckedExpression.Range -> {
                require(element is JaktRangeExpression)
                walkRange(element, checkedType.cast())
            }
            is CheckedExpression.Set -> {
                require(element is JaktSetExpression)
                walkSet(element, checkedType.cast())
            }
            is CheckedExpression.Tuple -> {
                require(element is JaktTupleExpression)
                walkTuple(element, checkedType.cast())
            }
            is CheckedExpression.UnaryOp -> {
                require(element is JaktUnaryExpression)
                walkUnaryOp(element, checkedType.cast())
            }
            is CheckedExpression.Var -> {
                require(element is JaktPlainQualifier)
                walkVar(element, checkedType.cast())
            }
        }
    }

    private fun walkArray(element: JaktArrayExpression, checkedType: CheckedExpression.Array) {
        val sizedBody = element.sizedArrayBody
        if (sizedBody != null) {
            walkExpression(sizedBody.findNotNullChildOfType(), checkedType.values[0])
            walkExpression(sizedBody.numericLiteral, checkedType.fillSize!!)
        } else {
            val elements = element.elementsArrayBody!!
            (elements.findChildrenOfType<JaktExpression>() zipSafe checkedType.values).forEach { (expr, type) ->
                walkExpression(expr, type)
            }
        }
    }

    private fun walkBinaryOp(element: JaktBinaryExpression, checkedType: CheckedExpression.BinaryOp) {
        walkExpression(element.left, checkedType.leftValue)
        walkExpression(element.right!!, checkedType.rightValue)
    }

    private fun walkCall(element: JaktCallExpression, checkedType: CheckedExpression.Call) {
        val bounds = element.genericBounds
        if (bounds != null)
            (bounds.typeList zipSafe checkedType.call.typeArgs).forEach { (type, id) -> walkType(type, id) }

        (element.argumentList!!.argumentList zipSafe checkedType.call.args).forEach { (arg, value) ->
            walkArgument(arg, value.second)
        }
    }

    private fun walkArgument(element: JaktArgument, checkedType: CheckedExpression) {
        val labeledArgument = element.labeledArgument
        if (labeledArgument != null) {
            walkExpression(labeledArgument.findNotNullChildOfType(), checkedType)
        } else {
            walkExpression(element.unlabeledArgument!!.findNotNullChildOfType(), checkedType)
        }
    }

    private fun walkDictionary(element: JaktDictionaryExpression, checkedType: CheckedExpression.Dictionary) {
        (element.dictionaryElementList zipSafe checkedType.values).forEach { (element, type) ->
            walkExpression(element.expressionList[0], type.first)
            walkExpression(element.expressionList[1], type.second)
        }
    }

    private fun walkIndexedDictionary(element: JaktIndexedAccessExpression, checkedType: CheckedExpression.IndexedDictionary) {
        walkExpression(element.expressionList[0], checkedType.value)
        walkExpression(element.expressionList[1], checkedType.index)
    }

    private fun walkIndexedExpression(element: JaktIndexedAccessExpression, checkedType: CheckedExpression.IndexedExpression) {
        walkExpression(element.expressionList[0], checkedType.value)
        walkExpression(element.expressionList[1], checkedType.index)
    }

    private fun walkIndexedStruct(element: JaktIndexedStructExpression, checkedType: CheckedExpression.IndexedStruct) {
        walkExpression(element.expression, checkedType.expression)
    }

    private fun walkIndexedTuple(element: JaktIndexedTupleExpression, checkedType: CheckedExpression.IndexedTuple) {
        walkExpression(element.expression, checkedType.expression)
    }

    private fun walkMatch(element: JaktMatchExpression, checkedType: CheckedExpression.Match) {
        walkExpression(element.findNotNullChildOfType(), checkedType.target)

        (element.matchPatternList zipSafe checkedType.cases).forEach { (pattern, type) ->
            walkMatchPattern(pattern, type)
        }
    }

    private fun walkMatchPattern(element: JaktMatchPattern, checkedType: CheckedMatchCase) {
        when (checkedType) {
            is CheckedMatchCase.CatchAll -> {}
            is CheckedMatchCase.EnumVariant -> {
                // TODO: Handle pattern head
            }
            is CheckedMatchCase.Expression -> walkExpression(
                element.patternHead.expressionPatternHead!!.findNotNullChildOfType(),
                checkedType.expression,
            )
        }

        walkMatchBody(element.patternTrail.matchBody, checkedType.body)
    }

    private fun walkMatchBody(element: JaktMatchBody, checkedType: CheckedMatchBody) {
        when (checkedType) {
            is CheckedMatchBody.Block -> walkBlock(element.block!!, checkedType.block)
            is CheckedMatchBody.Expression -> walkExpression(element.expression!!, checkedType.expression)
        }
    }

    private fun walkMethodCall(element: JaktCallExpression, checkedType: CheckedExpression.MethodCall) {
        walkExpression(element.expression, checkedType.receiver)

        val bounds = element.genericBounds
        if (bounds != null)
            (bounds.typeList zipSafe checkedType.call.typeArgs).forEach { (type, id) -> walkType(type, id) }

        (element.argumentList!!.argumentList zipSafe checkedType.call.args).forEach { (arg, value) ->
            walkArgument(arg, value.second)
        }
    }

    private fun walkOptionalNone(element: JaktOptionalNoneExpression, checkedType: CheckedExpression.OptionalNone) {
    }

    private fun walkOptionalSome(element: JaktOptionalSomeExpression, checkedType: CheckedExpression.OptionalSome) {
        walkExpression(element.findNotNullChildOfType(), checkedType.expression)
    }

    private fun walkRange(element: JaktRangeExpression, checkedType: CheckedExpression.Range) {
        walkExpression(element.expressionList[0], checkedType.from)
        walkExpression(element.expressionList[1], checkedType.to)
    }

    private fun walkSet(element: JaktSetExpression, checkedType: CheckedExpression.Set) {
        (element.findChildrenOfType<JaktExpression>() zipSafe checkedType.values).forEach { (expr, type) ->
            walkExpression(expr, type)
        }
    }

    private fun walkTuple(element: JaktTupleExpression, checkedType: CheckedExpression.Tuple) {
        (element.findChildrenOfType<JaktExpression>() zipSafe checkedType.values).forEach { (expr, type) ->
            walkExpression(expr, type)
        }
    }

    private fun walkUnaryOp(element: JaktUnaryExpression, checkedType: CheckedExpression.UnaryOp) {
        walkExpression(element.expression, checkedType.value)
    }

    private fun walkVar(element: JaktPlainQualifier, checkedType: CheckedExpression.Var) {
        val checkedVar = jaktProject.findVarInScope(currentScope, element.identifier.text) ?: return
        val source = elementTypeMap.getAssociatedElement<JaktDeclaration>(checkedVar)
        element.declaration = source
        source.declarationReferences!!.add(element)
    }

    private inline fun withScope(id: ScopeId, block: () -> Unit) {
        scopeStack.add(id)
        try {
            block()
        } finally {
            scopeStack.removeLast()
        }
    }
}
