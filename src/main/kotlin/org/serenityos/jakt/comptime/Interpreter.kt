package org.serenityos.jakt.comptime

import com.intellij.openapi.util.Ref
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.JaktScope
import org.serenityos.jakt.psi.ancestors
import org.serenityos.jakt.psi.api.*
import org.serenityos.jakt.psi.caching.comptimeCache

class Interpreter(element: JaktPsiElement) {
    var scope: Scope

    init {
        val outerScopes = element.ancestors().filter {
            it is JaktFile || it is JaktFunction || it is JaktStructDeclaration || it is JaktBlock
        }.map {
            val scope = Scope(null)
            if (it is JaktScope) {
                for (decl in it.getDeclarations()) {
                    try {
                        scope[decl.name ?: continue] = evaluate(decl)!!
                    } catch (e: Throwable) {
                        // Ignore and attempt to continue execution
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
    fun evaluate(element: JaktPsiElement): Value? {
        val cache = element.comptimeCache()
        cache.resolve<JaktPsiElement, Ref<Value>>(element)?.let { return it.get() }

        try {
            val value = evaluateImpl(element)
            cache.set(element, Ref(value))
            return value
        } catch (e: Throwable) {
            cache.set(element, Ref(null))
            throw e
        }
    }

    private fun evaluateImpl(element: JaktPsiElement): Value? {
        return when (element) {
            is JaktBooleanLiteral -> BoolValue(element.trueKeyword != null)
            is JaktNumericLiteral -> {
                element.binaryLiteral?.let {
                    return IntegerValue(it.text.toLong(2))
                }

                element.octalLiteral?.let {
                    return IntegerValue(it.text.toLong(8))
                }

                element.hexLiteral?.let {
                    return IntegerValue(it.text.toLong(16))
                }

                val decimalText = element.decimalLiteral!!.text
                return if ("." in decimalText) {
                    FloatValue(decimalText.toDouble())
                } else IntegerValue(decimalText.toLong(10))
            }
            is JaktLiteral -> {
                element.byteCharLiteral?.let {
                    return ByteCharValue(it.text[2].code.toByte())
                }

                element.charLiteral?.let {
                    return CharValue(it.text.single())
                }

                return StringValue(element.stringLiteral!!.text.drop(1).dropLast(1))
            }
            is JaktPlainQualifierExpression -> {
                val qualifier = element.plainQualifier

                val parts = generateSequence(qualifier) { it.plainQualifier }
                    .map { it.identifier.text }
                    .toMutableList()
                    .asReversed()

                var value: Value? = scope[parts[0]]
                parts.removeFirst()

                for (part in parts)
                   value = (value as StructValue)[part]

                value!!
            }
            is JaktTupleExpression -> TupleValue(element.expressionList.map { evaluate(it)!! })
            is JaktArrayExpression -> {
                element.elementsArrayBody?.let {  body ->
                    return ArrayValue(body.expressionList.map { evaluate(it)!! })
                }

                val body = element.sizedArrayBody!!
                val value = evaluate(body.expressionList[0])!!
                val size = evaluate(body.expressionList[1])!!
                check(size is IntegerValue)
                ArrayValue((0 until size.value).map { value })
            }
            is JaktSetExpression -> SetValue(element.expressionList.map { evaluate(it)!! }.toSet())
            is JaktDictionaryExpression -> DictionaryValue(
                element.dictionaryElementList.associate {
                    evaluate(it.expressionList[0])!! to evaluate(it.expressionList[1])!!
                }
            )
            is JaktFunction -> {
                val parameters = element.parameterList.parameterList.map { param ->
                    FunctionValue.Parameter(
                        param.identifier.text,
                        param.expression?.let { evaluate(it)!! }
                    )
                }

                UserFunctionValue(parameters, element.block ?: element.expression!!)
            }
            is JaktCallExpression -> {
                val target = evaluate(element.expression)!!
                check(target is FunctionValue)

                val args = element.argumentList.argumentList.map { evaluate(it.expression)!! }
                check(args.size in target.validParamCount)

                target.call(this, getThisValue(element.expression), args)
            }
            is JaktVariableDeclarationStatement -> {
                if (element.parenOpen != null)
                    TODO()

                val rhs = evaluate(element.expression)!!
                val name = element.variableDeclList[0].name!!
                scope[name] = rhs
                VoidValue
            }
            is JaktBlock -> {
                pushScope(Scope(scope))
                element.statementList.forEach(::evaluate)
                popScope()
                VoidValue
            }
            is JaktIfStatement -> {
                val canBeExpr = element.canBeExpr
                val condition = evaluate(element.expression)!!
                check(condition is BoolValue)

                if (condition.value) {
                    evaluate(element.block).let {
                        if (canBeExpr) it else VoidValue
                    }
                } else {
                    element.ifStatement?.let(::evaluate)
                        ?: element.elseBlock?.let(::evaluate)
                        ?: VoidValue
                }
            }
            is JaktAccessExpression -> {
                val target = evaluate(element.expression)!!
                if (element.dotQuestionMark != null)
                    TODO()
                if (element.decimalLiteral != null) {
                    (target as TupleValue).values[element.decimalLiteral!!.text.toInt()]
                } else {
                    (target as StructValue)[element.identifier!!.text]
                }
            }
            is JaktReturnStatement -> throw ReturnException(element.expression?.let { evaluate(it)!! })
            is JaktExpressionStatement -> {
                evaluate(element.expression)
                VoidValue
            }

            else -> TODO("${element::class.java} it not handled by the Interpreter")
        }
    }

    private fun getThisValue(expression: JaktExpression): Value? {
        return when (expression) {
            is JaktAccessExpression -> evaluate(expression.expression)!!
            is JaktFieldAccessExpression -> (scope as FunctionScope).thisBinding!!
            is JaktIndexedAccessExpression -> evaluate(expression.expressionList.first())!!
            else -> null
        }
    }

    private fun initializeGlobalScope(scope: Scope) {
        scope.initialize("StringBuilder", StringBuilderStruct)
    }

    open class Scope(var outer: Scope?) {
        protected val bindings = mutableMapOf<String, Value>()

        operator fun contains(name: String) = name in bindings

        operator fun get(name: String): Value? = bindings[name] ?: outer?.get(name)

        operator fun set(name: String, value: Value) {
            var scope: Scope? = this

            while (scope != null) {
                if (name in scope) {
                    scope[name] = value
                    return
                }
                scope = scope.outer
            }

            initialize(name, value)
        }

        fun initialize(name: String, value: Value) {
            bindings[name] = value
        }
    }

    class FunctionScope(outer: Scope?, val thisBinding: Value?) : Scope(outer) {
        fun argument(name: String) = bindings[name]!!
    }

    abstract class FlowException : Error()

    class ReturnException(val value: Value?) : FlowException()

    class YieldException(val value: Value?) : FlowException()

    companion object {
        fun evaluate(element: JaktPsiElement): Value? {
            return Interpreter(element).evaluate(element)
        }
    }
}
