package org.serenityos.jakt.comptime

import org.serenityos.jakt.psi.JaktPsiElement
import org.serenityos.jakt.psi.api.JaktExpression

// As with everything else in the plugin, we are very lenient when it comes to types.
// All integers are treated as i64, just to make my life easier. Similarly, all float
// types are treated as f64. If we produce a value for something that doesn't actually
// compile, we'll get an IDE error for it anyways
sealed interface Value

object VoidValue : Value {
    override fun toString() = "void"
}

data class BoolValue(val value: Boolean) : Value {
    override fun toString() = value.toString()
}

data class IntegerValue(val value: Long) : Value {
    override fun toString() = value.toString()
}

data class FloatValue(val value: Double) : Value {
    override fun toString() = value.toString()
}

data class CharValue(val value: Char) : Value {
    override fun toString() = "'$value'"
}

data class ByteCharValue(val value: Byte) : Value {
    override fun toString() = "b'$value'"
}

data class StringValue(val value: String) : Value {
    override fun toString() = "\"$value\""
}

data class TupleValue(val values: List<Value>) : Value {
    override fun toString() = values.joinToString(prefix = "(", postfix = ")")
}

data class ArrayValue(val values: List<Value>) : Value {
    override fun toString() = values.joinToString(prefix = "[", postfix = "]")
}

data class SetValue(val values: Set<Value>) : Value {
    override fun toString() = values.joinToString(prefix = "{", postfix = "}")
}

data class DictionaryValue(val elements: Map<Value, Value>) : Value {
    override fun toString() = elements.entries.joinToString(prefix = "{", postfix = "}") {
        "${it.key}: ${it.value}"
    }
}

open class StructValue : Value {
    protected val fieldsBacker = mutableMapOf<String, Value>()

    val fields: Map<String, Value>
        get() = fieldsBacker

    open operator fun get(name: String) = fields[name]
}

abstract class FunctionValue(val parameters: List<Parameter>) : Value {
    val validParamCount: IntRange
        get() = parameters.count { it.default != null }..parameters.size

    abstract fun call(interpreter: Interpreter, thisValue: Value?, arguments: List<Value>): Value

    data class Parameter(val name: String, val default: Value? = null)
}

class UserFunctionValue(
    parameters: List<Parameter>,
    val body: JaktPsiElement /* JaktBlock | JaktExpression */, // TODO: Storing PSI is bad, right?
) : FunctionValue(parameters) {
    override fun call(interpreter: Interpreter, thisValue: Value?, arguments: List<Value>): Value {
        val newScope = Interpreter.FunctionScope(interpreter.scope, thisValue)

        for ((index, param) in parameters.withIndex()) {
            if (index <= arguments.lastIndex) {
                newScope.initialize(param.name, arguments[index])
            } else {
                check(param.default != null)
                newScope.initialize(param.name, param.default)
            }
        }

        interpreter.pushScope(newScope)

        return try {
            interpreter.evaluate(body).let {
                if (body is JaktExpression) it!! else VoidValue
            }
        } catch (e: Interpreter.ReturnException) {
            e.value ?: VoidValue
        } catch (e: Interpreter.FlowException) {
            error("Unexpected FlowException in UserFunction: ${e::class.simpleName}")
        } finally {
            interpreter.popScope()
        }
    }
}
