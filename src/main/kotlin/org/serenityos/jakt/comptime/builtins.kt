package org.serenityos.jakt.comptime

class BuiltinFunction(
    parameters: List<Parameter>,
    private val func: (Value?, List<Value>) -> Value,
) : FunctionValue(parameters.toList()) {
    constructor(vararg parameters: Parameter, func: (Value?, List<Value>) -> Value) :
        this(parameters.toList(), func)

    constructor(vararg parameterNames: String, func: (Value?, List<Value>) -> Value) :
        this(parameterNames.map { Parameter(it) }, func)

    constructor(func: (Value?, List<Value>) -> Value) : this(emptyList(), func)

    override fun call(interpreter: Interpreter, thisValue: Value?, arguments: List<Value>): Value {
        return func(thisValue, arguments)
    }
}

object StringBuilderStruct : StructValue() {
    private val create = BuiltinFunction { thisValue, arguments ->
        require(thisValue == null)
        require(arguments.isEmpty())
        StringBuilderInstance()
    }

    init {
        fieldsBacker["create"] = create
    }
}

class StringBuilderInstance : StructValue() {
    val builder = StringBuilder()

    init {
        fieldsBacker["append"] = append
        fieldsBacker["append_string"] = appendString
        fieldsBacker["to_string"] = toString
    }

    companion object {
        private val append = BuiltinFunction("b") { thisValue, arguments ->
            require(thisValue is StringBuilderInstance)
            require(arguments.size == 1)
            val arg = arguments[0]
            require(arg is ByteCharValue)

            thisValue.builder.appendCodePoint(arg.value.toInt())
            VoidValue
        }

        private val appendString = BuiltinFunction("s") { thisValue, arguments ->
            require(thisValue is StringBuilderInstance)
            require(arguments.size == 1)
            val arg = arguments[0]
            require(arg is StringValue)

            thisValue.builder.append(arg.value)
            VoidValue
        }

        private val toString = BuiltinFunction { thisValue, arguments ->
            require(thisValue is StringBuilderInstance)
            require(arguments.isEmpty())
            StringValue(thisValue.builder.toString())
        }
    }
}
