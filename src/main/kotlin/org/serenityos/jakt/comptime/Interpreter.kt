package org.serenityos.jakt.comptime

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.JaktScope
import org.serenityos.jakt.psi.ancestors
import org.serenityos.jakt.psi.api.*
import org.serenityos.jakt.psi.findChildOfType
import org.serenityos.jakt.psi.reference.hasNamespace
import org.serenityos.jakt.utils.unreachable

class Interpreter(element: JaktPsiElement) {
    var scope: Scope

    val stdout = StringBuilder()
    val stderr = StringBuilder()

    init {
        val outerScopes = element.ancestors().filter {
            it is JaktFile || it is JaktFunction || it is JaktStructDeclaration || it is JaktBlock
        }.map {
            val scope = Scope(null)
            if (it is JaktScope) {
                for (decl in it.getDeclarations()) {
                    if (decl is JaktImportBraceEntry)
                        continue // TODO

                    scope[decl.name ?: continue] = when (val result = evaluate(decl)) {
                        is ExecutionResult.Normal -> result.value
                        else -> continue
                    }
                }
            }
            if (it is JaktFile)
                initializeGlobalScope(scope)
            scope
        }.toList()

        for ((index, scope) in outerScopes.withIndex()) {
            if (index == 0)
                continue
            scope.outer = outerScopes[index - 1]
        }

        scope = outerScopes.last()
    }

    fun pushScope(scope: Scope) {
        this.scope = scope
    }

    fun popScope() {
        scope = scope.outer!!
    }

    // A return value of null indicates the expression is not comptime. An exception being
    // thrown indicates the expression is comptime, but it malformed in some way and cannot
    // be evaluated.
    fun evaluate(element: JaktPsiElement): ExecutionResult {
        return when (element) {
            /*** EXPRESSIONS ***/

            is JaktMatchExpression -> TODO()
            is JaktTryExpression -> TODO()
            is JaktLambdaExpression -> TODO()
            is JaktAssignmentBinaryExpression -> {
                val binaryOp = when {
                    element.plusEquals != null -> BinaryOperator.Add
                    element.minusEquals != null -> BinaryOperator.Subtract
                    element.asteriskEquals != null -> BinaryOperator.Multiply
                    element.slashEquals != null -> BinaryOperator.Divide
                    element.percentEquals != null -> BinaryOperator.Modulo
                    element.arithLeftShiftEquals != null -> BinaryOperator.ArithLeftShift
                    element.leftShiftEquals != null -> BinaryOperator.LeftShift
                    element.arithRightShiftEquals != null -> BinaryOperator.ArithRightShift
                    element.rightShiftEquals != null -> BinaryOperator.RightShift
                    else -> null
                }

                val newValue = if (binaryOp != null) {
                    applyBinaryOperator(element.left, element.right!!, binaryOp).let {
                        when (it) {
                            is ExecutionResult.Normal -> it.value
                            is ExecutionResult.Yield -> error(
                                "Unexpected yield",
                                TextRange(element.left.startOffset, element.right!!.endOffset),
                            )
                            else -> return it
                        }
                    }
                } else {
                    evaluate(element.right!!).let {
                        when (it) {
                            is ExecutionResult.Normal -> it.value
                            is ExecutionResult.Yield -> error("Unexpected yield", element.right!!)
                            else -> return it
                        }
                    }
                }

                when (val assignmentTarget = element.left) {
                    is JaktPlainQualifierExpression -> {
                        if (assignmentTarget.plainQualifier.hasNamespace)
                            error("Invalid assignment target", assignmentTarget)

                        val name = assignmentTarget.plainQualifier.name!!
                        if (!assign(name, newValue, initialize = false))
                            error("Unknown identifier \"$name\"", assignmentTarget)
                    }
                    is JaktIndexedAccessExpression -> {
                        val target = when (val result = evaluate(element.left)) {
                            is ExecutionResult.Normal -> result.value
                            is ExecutionResult.Yield -> error("Unexpected yield", element.left)
                            else -> return result
                        }

                        if (target !is ArrayValue)
                            error("Expected array, found ${target.typeName()}", element.left)

                        val index = when (val result = evaluate(element.right!!)) {
                            is ExecutionResult.Normal -> result.value
                            is ExecutionResult.Yield -> error("Unexpected yield", element.right!!)
                            else -> return result
                        }

                        if (index !is IntegerValue)
                            error("Expected integer, found ${index.typeName()}", element.right!!)

                        if (index.value.toInt() > target.values.size)
                            error(
                                "Out-of-bounds assignment to array of length ${target.values.size} with index ${index.value}",
                                assignmentTarget
                            )

                        target.values[index.value.toInt()] = newValue
                    }
                    is JaktAccessExpression -> {
                        val target = when (val result = evaluate(element.left)) {
                            is ExecutionResult.Normal -> result.value
                            is ExecutionResult.Yield -> error("Unexpected yield", element.left)
                            else -> return result
                        }

                        if (assignmentTarget.decimalLiteral != null) {
                            if (target !is TupleValue)
                                error("Expected tuple, found ${target.typeName()}", element.left)

                            val index = assignmentTarget.decimalLiteral!!.text.toInt()
                            if (index > target.values.size)
                                error("Cannot assign to index $index of tuple of length ${target.values.size}")

                            target.values[index] = newValue
                        } else {
                            target[assignmentTarget.identifier!!.text] = newValue
                        }
                    }
                    else -> error("Invalid assignment target", assignmentTarget)
                }

                ExecutionResult.Normal(VoidValue)
            }
            is JaktThisExpression -> TODO()
            is JaktFieldAccessExpression -> TODO()
            is JaktRangeExpression -> {
                val (startExpr, endExpr) = when {
                    element.expressionList.size == 2 -> element.expressionList[0] to element.expressionList[1]
                    element.expressionList.isEmpty() -> null to null
                    element.expressionList[0].textRange.endOffset < element.dotDot.textRange.startOffset ->
                        element.expressionList[0] to null
                    else -> null to element.expressionList[0]
                }

                val start = startExpr?.let {
                    when (val result = evaluate(it)) {
                        is ExecutionResult.Normal -> result.value
                        is ExecutionResult.Yield -> error("Unexpected yield", it)
                        else -> return result
                    }
                } ?: IntegerValue(0)

                val end = startExpr?.let {
                    when (val result = evaluate(it)) {
                        is ExecutionResult.Normal -> result.value
                        is ExecutionResult.Yield -> error("Unexpected yield", it)
                        else -> return result
                    }
                } ?: IntegerValue(Long.MAX_VALUE)

                if (start !is IntegerValue)
                    error("Expected range start value to be an integer", startExpr!!)

                if (end !is IntegerValue)
                    error("Expected range end value to be an integer", endExpr!!)

                ExecutionResult.Normal(RangeValue(start.value, end.value, isInclusive = false))
            }
            is JaktLogicalOrBinaryExpression -> applyBinaryOperator(
                element.left,
                element.right!!,
                BinaryOperator.LogicalOr
            )
            is JaktLogicalAndBinaryExpression -> applyBinaryOperator(
                element.left,
                element.right!!,
                BinaryOperator.LogicalAnd
            )
            is JaktBitwiseOrBinaryExpression -> applyBinaryOperator(
                element.left,
                element.right!!,
                BinaryOperator.BitwiseOr
            )
            is JaktBitwiseXorBinaryExpression -> applyBinaryOperator(
                element.left,
                element.right!!,
                BinaryOperator.BitwiseXor
            )
            is JaktBitwiseAndBinaryExpression -> applyBinaryOperator(
                element.left,
                element.right!!,
                BinaryOperator.BitwiseAnd
            )
            is JaktRelationalBinaryExpression -> applyBinaryOperator(
                element.left,
                element.right!!,
                when {
                    element.doubleEquals != null -> BinaryOperator.Equals
                    element.notEquals != null -> BinaryOperator.NotEquals
                    element.greaterThan != null -> BinaryOperator.GreaterThan
                    element.greaterThanEquals != null -> BinaryOperator.GreaterThanEq
                    element.lessThan != null -> BinaryOperator.LessThan
                    else -> BinaryOperator.LessThanEq
                },
            )
            is JaktShiftBinaryExpression -> applyBinaryOperator(
                element.left,
                element.right!!,
                when {
                    element.leftShift != null -> BinaryOperator.LeftShift
                    element.arithLeftShift != null -> BinaryOperator.ArithLeftShift
                    element.rightShift != null -> BinaryOperator.RightShift
                    else -> BinaryOperator.ArithRightShift
                },
            )
            is JaktAddBinaryExpression -> applyBinaryOperator(
                element.left,
                element.right!!,
                if (element.plus != null) BinaryOperator.Add else BinaryOperator.Subtract,
            )
            is JaktMultiplyBinaryExpression -> applyBinaryOperator(
                element.left,
                element.right!!,
                when {
                    element.asterisk != null -> BinaryOperator.Multiply
                    element.slash != null -> BinaryOperator.Divide
                    else -> BinaryOperator.Modulo
                },
            )
            is JaktCastExpression -> TODO()
            is JaktIsExpression -> TODO()
            is JaktUnaryExpression -> TODO()
            is JaktBooleanLiteral -> ExecutionResult.Normal(BoolValue(element.trueKeyword != null))
            is JaktNumericLiteral -> {
                element.binaryLiteral?.let {
                    return ExecutionResult.Normal(IntegerValue(it.text.toLong(2)))
                }

                element.octalLiteral?.let {
                    return ExecutionResult.Normal(IntegerValue(it.text.toLong(8)))
                }

                element.hexLiteral?.let {
                    return ExecutionResult.Normal(IntegerValue(it.text.toLong(16)))
                }

                val decimalText = element.decimalLiteral!!.text
                return ExecutionResult.Normal(
                    if ("." in decimalText) {
                        FloatValue(decimalText.toDouble())
                    } else IntegerValue(decimalText.toLong(10))
                )
            }
            is JaktLiteral -> {
                element.byteCharLiteral?.let {
                    return ExecutionResult.Normal(ByteCharValue(it.text[2].code.toByte()))
                }

                element.charLiteral?.let {
                    return ExecutionResult.Normal(CharValue(it.text.single()))
                }

                ExecutionResult.Normal(StringValue(element.stringLiteral!!.text.drop(1).dropLast(1)))
            }
            is JaktAccessExpression -> {
                val target = when (val result = evaluate(element.expression)) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", element.expression)
                    else -> return result
                }

                if (element.dotQuestionMark != null)
                    TODO()

                if (element.decimalLiteral != null) {
                    if (target !is TupleValue)
                        error("Invalid tuple index into non-tuple value", element.decimalLiteral!!)

                    ExecutionResult.Normal(target.values[element.decimalLiteral!!.text.toInt()])
                } else {
                    val value = target[element.identifier!!.text] ?: error("Unknown field ${element.identifier!!.text}")
                    ExecutionResult.Normal(value)
                }
            }
            is JaktIndexedAccessExpression -> {
                val target = when (val result = evaluate(element.expressionList[0])) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", element.expressionList[0])
                    else -> return result
                }

                val value = when (val result = evaluate(element.expressionList[1])) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", element.expressionList[1])
                    else -> return result
                }

                if (target !is ArrayValue)
                    error("Unexpected index into non-array value", element.expressionList[0])

                when (value) {
                    is RangeValue -> ExecutionResult.Normal(ArraySlice(target, value.range))
                    is IntegerValue -> {
                        val result = target.values.getOrNull(value.value.toInt())
                            ?: error("Index ${value.value} out-of-range for array of length ${target.values.size}")
                        ExecutionResult.Normal(result)
                    }
                    else -> error("Expected integer or range in array indexing expression", element.expressionList[1])
                }
            }
            is JaktPlainQualifierExpression -> {
                val qualifier = element.plainQualifier

                val parts = generateSequence(qualifier) { it.plainQualifier }
                    .map { it to it.identifier.text }
                    .toMutableList()
                    .asReversed()

                var value: Value? = null
                var currScope: Scope? = scope

                while (currScope != null) {
                    if (parts[0].second in currScope) {
                        value = scope[parts[0].second]
                        parts.removeFirst()
                        break
                    }

                    currScope = currScope.outer
                }

                if (value == null) {
                    val type = if (parts.size > 1) "qualifier" else "identifier"
                    error("Unknown $type \"${parts[0].second}\"", parts[0].first)
                }

                for (part in parts) {
                    if (part.second !in value!!)
                        error("\"${value.typeName()}\" has no member named ${part.second}", part.first)

                    value = value[part.second]!!
                }

                ExecutionResult.Normal(value!!)
            }
            is JaktCallExpression -> {
                val target = when (val result = evaluate(element.expression)) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", element.expression)
                    else -> return result
                }

                if (target !is FunctionValue)
                    error("\"${target.typeName()}\" is not callable", element.expression)

                val args = element.argumentList.argumentList.map {
                    when (val result = evaluate(it.expression)) {
                        is ExecutionResult.Normal -> result.value
                        is ExecutionResult.Yield -> error("Unexpected yield", element.expression)
                        else -> return result
                    }
                }

                if (args.size !in target.validParamCount) {
                    error(
                        "Expected between ${target.validParamCount.first} and ${target.validParamCount.last} " +
                            "arguments, but found ${args.size} arguments",
                        element.argumentList
                    )
                }

                val thisValue = when (val expr = element.expression) {
                    is JaktAccessExpression -> when (val result = evaluate(expr.expression)) {
                        is ExecutionResult.Normal -> result.value
                        is ExecutionResult.Yield -> error("Unexpected yield", expr.expression)
                        else -> return result
                    }
                    is JaktFieldAccessExpression -> (scope as FunctionScope).thisBinding!!
                    is JaktIndexedAccessExpression -> when (val result = evaluate(expr.expressionList.first())) {
                        is ExecutionResult.Normal -> result.value
                        is ExecutionResult.Yield -> error("Unexpected yield", element.expression)
                        else -> return result
                    }
                    else -> null
                }

                target.call(this, thisValue, args)
            }
            is JaktArrayExpression -> {
                element.elementsArrayBody?.let { body ->
                    val array = ArrayValue(body.expressionList.map {
                        when (val result = evaluate(it)) {
                            is ExecutionResult.Normal -> result.value
                            is ExecutionResult.Yield -> error("Unexpected yield", it)
                            else -> return result
                        }
                    }.toMutableList())

                    return ExecutionResult.Normal(array)
                }

                val body = element.sizedArrayBody!!

                val value = when (val result = evaluate(body.expressionList[0])) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", body.expressionList[0])
                    else -> return result
                }

                val size = when (val result = evaluate(body.expressionList[1])) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", body.expressionList[1])
                    else -> return result
                }

                if (size !is IntegerValue)
                    error("Array size initializer must be an integer", body.expressionList[1])

                ExecutionResult.Normal(ArrayValue((0 until size.value).map { value }.toMutableList()))
            }
            is JaktDictionaryExpression -> ExecutionResult.Normal(
                DictionaryValue(
                    element.dictionaryElementList.associate {
                        val key = when (val result = evaluate(it.expressionList[0])) {
                            is ExecutionResult.Normal -> result.value
                            is ExecutionResult.Yield -> error("Unexpected yield", it.expressionList[0])
                            else -> return result
                        }

                        val value = when (val result = evaluate(it.expressionList[1])) {
                            is ExecutionResult.Normal -> result.value
                            is ExecutionResult.Yield -> error("Unexpected yield", it.expressionList[1])
                            else -> return result
                        }

                        key to value
                    }.toMutableMap()
                )
            )
            is JaktSetExpression -> ExecutionResult.Normal(SetValue(element.expressionList.map {
                when (val result = evaluate(it)) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", it)
                    else -> return result
                }
            }.toMutableSet()))
            is JaktTupleExpression -> ExecutionResult.Normal(TupleValue(element.expressionList.map {
                when (val result = evaluate(it)) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", it)
                    else -> return result
                }
            }.toMutableList()))

            /*** STATEMENTS ***/

            is JaktExpressionStatement -> when (val result = evaluate(element.expression)) {
                is ExecutionResult.Normal -> ExecutionResult.Normal(result.value)
                is ExecutionResult.Yield -> error("Unexpected yield", element.expression)
                else -> return result
            }
            is JaktReturnStatement -> ExecutionResult.Return(element.expression?.let {
                when (val result = evaluate(it)) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", it)
                    else -> return result
                }
            } ?: VoidValue)
            is JaktThrowStatement -> ExecutionResult.Throw(when (val result = evaluate(element.expression)) {
                is ExecutionResult.Normal -> result.value
                is ExecutionResult.Yield -> error("Unexpected yield", element.expression)
                else -> return result
            })
            is JaktDeferStatement -> TODO()
            is JaktIfStatement -> {
                val condition = when (val result = evaluate(element.expression)) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", element.expression)
                    else -> return result
                }

                if (condition !is BoolValue)
                    error("Expected bool", element.expression)

                if (condition.value) {
                    when (val result = evaluate(element.block)) {
                        is ExecutionResult.Normal -> ExecutionResult.Normal(VoidValue)
                        is ExecutionResult.Yield -> error(
                            "Unexpected yield",
                            element.block.findChildOfType<JaktYieldStatement>()!!
                        )
                        else -> return result
                    }
                } else if (element.ifStatement != null) {
                    evaluate(element.ifStatement!!)
                } else if (element.elseBlock != null) {
                    when (val result = evaluate(element.elseBlock!!)) {
                        is ExecutionResult.Normal -> ExecutionResult.Normal(VoidValue)
                        is ExecutionResult.Yield -> error(
                            "Unexpected yield",
                            element.block.findChildOfType<JaktYieldStatement>()!!
                        )
                        else -> return result
                    }
                } else ExecutionResult.Normal(VoidValue)
            }
            is JaktWhileStatement -> TODO()
            is JaktLoopStatement -> TODO()
            is JaktForStatement -> TODO()
            is JaktVariableDeclarationStatement -> {
                if (element.parenOpen != null) {
                    error(
                        "Destructuring variable assignments are not supported",
                        TextRange(element.parenOpen!!.startOffset, element.parenClose!!.endOffset),
                    )
                }

                val rhs = when (val result = evaluate(element.expression)) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", element.expression)
                    else -> return result
                }

                assign(element.variableDeclList[0].name!!, rhs, initialize = true)

                ExecutionResult.Normal(VoidValue)
            }
            is JaktGuardStatement -> TODO()
            is JaktYieldStatement -> ExecutionResult.Yield(
                when (val result = evaluate(element.expression)) {
                    is ExecutionResult.Normal -> result.value
                    is ExecutionResult.Yield -> error("Unexpected yield", element.expression)
                    else -> return result
                }
            )
            is JaktBreakStatement -> ExecutionResult.Break
            is JaktContinueStatement -> ExecutionResult.Continue
            is JaktUnsafeStatement -> error("Cannot evaluate unsafe blocks at comptime", element)
            is JaktInlineCppStatement -> error("Cannot evaluate inline cpp blocks at comptime")
            is JaktBlock -> {
                pushScope(Scope(scope))
                try {
                    element.statementList.forEach {
                        val result = evaluate(it)
                        if (result is ExecutionResult.Yield || result is ExecutionResult.Return)
                            return result
                    }
                    ExecutionResult.Normal(VoidValue)
                } finally {
                    popScope()
                }
            }
            is JaktFunction -> {
                val parameters = element.parameterList.parameterList.map { param ->
                    val default = param.expression?.let {
                        when (val result = evaluate(it)) {
                            is ExecutionResult.Normal -> result.value
                            is ExecutionResult.Yield -> error("Unexpected yield", it)
                            else -> return result
                        }
                    }

                    FunctionValue.Parameter(param.identifier.text, default)
                }

                val target = element.block ?: element.expression ?: return ExecutionResult.Normal(VoidValue)
                ExecutionResult.Normal(UserFunctionValue(parameters, target))
            }

            // Ignored declarations (hoisted at scope initialization)
            is JaktImport -> ExecutionResult.Normal(VoidValue)

            else -> error("${element::class.simpleName} is not support at comptime")
        }
    }

    private fun applyBinaryOperator(
        lhsExpr: JaktExpression,
        rhsExpr: JaktExpression,
        op: BinaryOperator
    ): ExecutionResult {
        if (op == BinaryOperator.LogicalOr || op == BinaryOperator.LogicalAnd) {
            val shortCircuitValue = op == BinaryOperator.LogicalOr

            val lhsValue = when (val result = evaluate(lhsExpr)) {
                is ExecutionResult.Normal -> result.value
                else -> return result
            }
            if (lhsValue !is BoolValue)
                error("Expected bool, found ${lhsValue.typeName()}", lhsExpr)

            if (lhsValue.value == shortCircuitValue)
                return ExecutionResult.Normal(BoolValue(shortCircuitValue))

            val rhsValue = when (val result = evaluate(rhsExpr)) {
                is ExecutionResult.Normal -> result.value
                else -> return result
            }
            if (rhsValue !is BoolValue)
                error("Expected bool, found ${rhsValue.typeName()}", rhsExpr)

            ExecutionResult.Normal(rhsValue)
        }

        val lhsValue = when (val result = evaluate(lhsExpr)) {
            is ExecutionResult.Normal -> result.value
            else -> return result
        }

        val rhsValue = when (val result = evaluate(rhsExpr)) {
            is ExecutionResult.Normal -> result.value
            else -> return result
        }

        fun incompatError(): Nothing {
            error(
                "Incompatible types \"${lhsValue.typeName()}\" and \"${rhsValue.typeName()}\" for operator ${op.op}",
                TextRange(lhsExpr.textRange.startOffset, rhsExpr.textRange.endOffset)
            )
        }

        if (lhsValue.typeName() != rhsValue.typeName())
            incompatError()

        // TODO: Separator integer types

        val value = when (op) {
            BinaryOperator.BitwiseOr,
            BinaryOperator.BitwiseXor,
            BinaryOperator.BitwiseAnd,
            BinaryOperator.LeftShift,
            BinaryOperator.RightShift,
            BinaryOperator.ArithLeftShift,
            BinaryOperator.ArithRightShift -> {
                if (lhsValue !is IntegerValue || rhsValue !is IntegerValue)
                    incompatError()

                val value = when (op) {
                    BinaryOperator.BitwiseOr -> lhsValue.value or rhsValue.value
                    BinaryOperator.BitwiseXor -> lhsValue.value xor rhsValue.value
                    BinaryOperator.BitwiseAnd -> lhsValue.value and rhsValue.value
                    BinaryOperator.LeftShift, BinaryOperator.ArithLeftShift -> lhsValue.value shl rhsValue.value.toInt()
                    BinaryOperator.RightShift, BinaryOperator.ArithRightShift -> lhsValue.value shr rhsValue.value.toInt()
                    else -> unreachable()
                }

                IntegerValue(value)
            }
            BinaryOperator.Add,
            BinaryOperator.Subtract,
            BinaryOperator.Multiply,
            BinaryOperator.Divide,
            BinaryOperator.Modulo -> {
                val lhsNum = when (lhsValue) {
                    is IntegerValue -> lhsValue.value.toDouble()
                    is FloatValue -> lhsValue.value
                    else -> incompatError()
                }

                val rhsNum = when (rhsValue) {
                    is IntegerValue -> rhsValue.value.toDouble()
                    is FloatValue -> rhsValue.value
                    else -> incompatError()
                }

                val result = when (op) {
                    BinaryOperator.Add -> lhsNum + rhsNum
                    BinaryOperator.Subtract -> lhsNum - rhsNum
                    BinaryOperator.Multiply -> lhsNum * rhsNum
                    BinaryOperator.Divide -> lhsNum / rhsNum
                    BinaryOperator.Modulo -> lhsNum % rhsNum
                    else -> unreachable()
                }

                if (lhsValue is IntegerValue) {
                    IntegerValue(result.toLong())
                } else FloatValue(result)
            }
            BinaryOperator.Equals -> when (lhsValue) {
                is BoolValue -> BoolValue(lhsValue.value == (rhsValue as BoolValue).value)
                is IntegerValue -> BoolValue(lhsValue.value == (rhsValue as IntegerValue).value)
                is FloatValue -> BoolValue(lhsValue.value == (rhsValue as FloatValue).value)
                is CharValue -> BoolValue(lhsValue.value == (rhsValue as CharValue).value)
                is ByteCharValue -> BoolValue(lhsValue.value == (rhsValue as ByteCharValue).value)
                is StringValue -> BoolValue(lhsValue.value == (rhsValue as StringValue).value)
                else -> incompatError()
            }
            BinaryOperator.NotEquals -> when (lhsValue) {
                is BoolValue -> BoolValue(lhsValue.value != (rhsValue as BoolValue).value)
                is IntegerValue -> BoolValue(lhsValue.value != (rhsValue as IntegerValue).value)
                is FloatValue -> BoolValue(lhsValue.value != (rhsValue as FloatValue).value)
                is CharValue -> BoolValue(lhsValue.value != (rhsValue as CharValue).value)
                is ByteCharValue -> BoolValue(lhsValue.value != (rhsValue as ByteCharValue).value)
                is StringValue -> BoolValue(lhsValue.value != (rhsValue as StringValue).value)
                else -> incompatError()
            }
            BinaryOperator.GreaterThan -> when (lhsValue) {
                is BoolValue -> BoolValue(lhsValue.value > (rhsValue as BoolValue).value)
                is IntegerValue -> BoolValue(lhsValue.value > (rhsValue as IntegerValue).value)
                is FloatValue -> BoolValue(lhsValue.value > (rhsValue as FloatValue).value)
                is CharValue -> BoolValue(lhsValue.value > (rhsValue as CharValue).value)
                is ByteCharValue -> BoolValue(lhsValue.value > (rhsValue as ByteCharValue).value)
                is StringValue -> BoolValue(lhsValue.value > (rhsValue as StringValue).value)
                else -> incompatError()
            }
            BinaryOperator.GreaterThanEq -> when (lhsValue) {
                is BoolValue -> BoolValue(lhsValue.value >= (rhsValue as BoolValue).value)
                is IntegerValue -> BoolValue(lhsValue.value >= (rhsValue as IntegerValue).value)
                is FloatValue -> BoolValue(lhsValue.value >= (rhsValue as FloatValue).value)
                is CharValue -> BoolValue(lhsValue.value >= (rhsValue as CharValue).value)
                is ByteCharValue -> BoolValue(lhsValue.value >= (rhsValue as ByteCharValue).value)
                is StringValue -> BoolValue(lhsValue.value >= (rhsValue as StringValue).value)
                else -> incompatError()
            }
            BinaryOperator.LessThan -> when (lhsValue) {
                is BoolValue -> BoolValue(lhsValue.value < (rhsValue as BoolValue).value)
                is IntegerValue -> BoolValue(lhsValue.value < (rhsValue as IntegerValue).value)
                is FloatValue -> BoolValue(lhsValue.value < (rhsValue as FloatValue).value)
                is CharValue -> BoolValue(lhsValue.value < (rhsValue as CharValue).value)
                is ByteCharValue -> BoolValue(lhsValue.value < (rhsValue as ByteCharValue).value)
                is StringValue -> BoolValue(lhsValue.value < (rhsValue as StringValue).value)
                else -> incompatError()
            }
            BinaryOperator.LessThanEq -> when (lhsValue) {
                is BoolValue -> BoolValue(lhsValue.value <= (rhsValue as BoolValue).value)
                is IntegerValue -> BoolValue(lhsValue.value <= (rhsValue as IntegerValue).value)
                is FloatValue -> BoolValue(lhsValue.value <= (rhsValue as FloatValue).value)
                is CharValue -> BoolValue(lhsValue.value <= (rhsValue as CharValue).value)
                is ByteCharValue -> BoolValue(lhsValue.value <= (rhsValue as ByteCharValue).value)
                is StringValue -> BoolValue(lhsValue.value <= (rhsValue as StringValue).value)
                else -> incompatError()
            }
            else -> unreachable()
        }

        return ExecutionResult.Normal(value)
    }

    private fun assign(name: String, value: Value, initialize: Boolean): Boolean {
        // TODO: Ensure bindings already exists in the scope

        var currScope: Scope? = scope
        while (currScope != null) {
            if (name in currScope) {
                currScope[name] = value
                return true
            }

            currScope = currScope.outer
        }

        return if (initialize) {
            scope[name] = value
            true
        } else false
    }

    private fun initializeGlobalScope(scope: Scope) {
        scope["String"] = StringStruct
        scope["StringBuilder"] = StringBuilderStruct
        scope["Error"] = ErrorStruct
        scope["File"] = FileStruct
        scope["___jakt_get_target_triple_string"] = jaktGetTargetTripleStringFunction
        scope["abort"] = abortFunction
        scope["format"] = FormatFunction
        scope["print"] = PrintFunction
        scope["println"] = PrintlnFunction
        scope["eprint"] = EprintFunction
        scope["eprintln"] = EprintlnFunction
    }

    fun error(message: String, element: PsiElement): Nothing = error(message, element.textRange)

    fun error(message: String, range: TextRange): Nothing = throw InterpreterException(message, range)

    enum class BinaryOperator(val op: String) {
        LogicalOr("or"),
        LogicalAnd("and"),
        BitwiseOr("|"),
        BitwiseXor("^"),
        BitwiseAnd("&"),
        LeftShift("<<"),
        RightShift(">>"),
        ArithLeftShift("<<<"),
        ArithRightShift(">>>"),
        Equals("=="),
        NotEquals("!="),
        GreaterThan(">"),
        GreaterThanEq(">="),
        LessThan("<"),
        LessThanEq("<="),
        Add("+"),
        Subtract("-"),
        Multiply("*"),
        Divide("/"),
        Modulo("%"),
    }

    open class Scope(var outer: Scope?) {
        protected val bindings = mutableMapOf<String, Value>()

        operator fun contains(name: String) = name in bindings

        operator fun get(name: String): Value? = bindings[name] ?: outer?.get(name)

        operator fun set(name: String, value: Value) {
            bindings[name] = value
        }
    }

    class FunctionScope(outer: Scope?, val thisBinding: Value?) : Scope(outer) {
        fun argument(name: String) = bindings[name]!!
    }

    sealed interface ExecutionResult {
        class Return(val value: Value) : ExecutionResult

        class Yield(val value: Value) : ExecutionResult

        class Throw(val value: Value) : ExecutionResult

        class Normal(val value: Value) : ExecutionResult

        object Continue : ExecutionResult

        object Break : ExecutionResult
    }

    data class Result(
        val value: Value?,
        val stdout: String,
        val stderr: String,
    )

    companion object {
        fun evaluate(element: JaktPsiElement): Result {
            val interpreter = Interpreter(element)

            val value = when (val result = interpreter.evaluate(element)) {
                is ExecutionResult.Normal -> result.value
                else -> null
            }

            return Result(value, interpreter.stdout.toString(), interpreter.stderr.toString())
        }
    }
}
