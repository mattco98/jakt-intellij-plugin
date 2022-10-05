package org.serenityos.jakt.comptime

import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.api.JaktExpression

// As with everything else in the plugin, we are very lenient when it comes to types.
// All integers are treated as i64, just to make my life easier. Similarly, all float
// types are treated as f64. If we produce a value for something that doesn't actually
// compile, we'll get an IDE error for it anyways
sealed class Value {
    private val fields = mutableMapOf<String, Value>()

    abstract fun typeName(): String

    open operator fun contains(name: String) = name in fields

    open operator fun get(name: String) = fields[name]

    open operator fun set(name: String, value: Value) {
        fields[name] = value
    }
}

interface PrimitiveValue {
    val value: Any
}

object VoidValue : Value() {
    override fun typeName() = "void"
    override fun toString() = "void"
}

data class BoolValue(override val value: Boolean) : Value(), PrimitiveValue {
    override fun typeName() = "bool"
    override fun toString() = value.toString()
}

data class IntegerValue(override val value: Long) : Value(), PrimitiveValue {
    override fun typeName() = "i64"
    override fun toString() = value.toString()
}

data class FloatValue(override val value: Double) : Value(), PrimitiveValue {
    override fun typeName() = "f64"
    override fun toString() = value.toString()
}

data class CharValue(override val value: Char) : Value(), PrimitiveValue {
    override fun typeName() = "c_char"
    override fun toString() = "'$value'"
}

data class ByteCharValue(override val value: Byte) : Value(), PrimitiveValue {
    override fun typeName() = "u8"
    override fun toString() = "b'$value'"
}

data class TupleValue(val values: MutableList<Value>) : Value() {
    override fun typeName() = "Tuple"
    override fun toString() = values.joinToString(prefix = "(", postfix = ")")
}

abstract class FunctionValue(private val minParamCount: Int, private val maxParamCount: Int) : Value() {
    val validParamCount: IntRange
        get() = minParamCount..maxParamCount

    init {
        require(minParamCount <= maxParamCount)
    }

    override fun typeName() = "function"

    constructor(parameters: List<Parameter>) : this(parameters.count { it.default == null }, parameters.size)

    abstract fun call(interpreter: Interpreter, thisValue: Value?, arguments: List<Value>): Interpreter.ExecutionResult

    data class Parameter(val name: String, val default: Value? = null)
}

class UserFunctionValue(
    private val parameters: List<Parameter>,
    val body: JaktPsiElement /* JaktBlock | JaktExpression */, // TODO: Storing PSI is bad, right?
) : FunctionValue(parameters) {
    override fun call(interpreter: Interpreter, thisValue: Value?, arguments: List<Value>): Interpreter.ExecutionResult {
        val newScope = Interpreter.FunctionScope(interpreter.scope, thisValue)

        for ((index, param) in parameters.withIndex()) {
            if (index <= arguments.lastIndex) {
                newScope[param.name] = arguments[index]
            } else {
                check(param.default != null)
                newScope[param.name] = param.default
            }
        }

        interpreter.pushScope(newScope)

        return when (val result = interpreter.evaluate(body)) {
            is Interpreter.ExecutionResult.Normal -> if (body is JaktExpression) {
                Interpreter.ExecutionResult.Normal(result.value)
            } else Interpreter.ExecutionResult.Normal(VoidValue)
            is Interpreter.ExecutionResult.Return -> Interpreter.ExecutionResult.Normal(result.value)
            is Interpreter.ExecutionResult.Throw -> result
            else -> interpreter.error("Unexpected control flow", body)
        }.also {
            interpreter.popScope()
        }
    }
}
