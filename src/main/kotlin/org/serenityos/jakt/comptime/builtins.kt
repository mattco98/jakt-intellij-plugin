package org.serenityos.jakt.comptime

import org.serenityos.jakt.project.JaktProjectListener
import java.io.File

class BuiltinFunction(
    parameterCount: Int,
    private val func: (Value?, List<Value>) -> Value,
) : FunctionValue(parameterCount, parameterCount) {
    override fun call(interpreter: Interpreter, thisValue: Value?, arguments: List<Value>): Interpreter.ExecutionResult {
        return Interpreter.ExecutionResult.Normal(func(thisValue, arguments))
    }
}

data class OptionalValue(val value: Value?) : Value() {
    init {
        this["has_value"] = hasValue
        this["value"] = getValue
        this["value_or"] = getValueOr
    }

    override fun typeName() = "Optional"

    override fun toString() = "Optional(${value?.toString() ?: "<empty>"})"

    companion object {
        private val hasValue = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is OptionalValue)
            BoolValue(thisValue.value != null)
        }

        private val getValue = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is OptionalValue)
            thisValue.value!!
        }

        private val getValueOr = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is OptionalValue)
            thisValue.value ?: arguments[0]
        }
    }
}

data class ArrayIterator(
    val array: ArrayValue,
    private var nextIndex: Int = 0,
    private val endInclusiveIndex: Int = array.values.lastIndex,
) : Value() {
    init {
        this["next"] = next
    }

    override fun typeName() = "ArrayIterator"

    override fun toString() = "ArrayIterator($nextIndex..$endInclusiveIndex)"

    companion object {
        private val next = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArrayIterator)
            if (thisValue.nextIndex > thisValue.endInclusiveIndex) {
                OptionalValue(null)
            } else OptionalValue(thisValue.array.values[thisValue.nextIndex]).also {
                thisValue.nextIndex += 1
            }
        }
    }
}

data class ArrayValue(val values: MutableList<Value>) : Value() {
    init {
        this["is_empty"] = isEmpty
        this["size"] = size
        this["contains"] = contains
        this["iterator"] = iterator
        this["push"] = push
        this["pop"] = pop
        this["first"] = first
        this["last"] = last
    }

    override fun typeName() = "Array"

    override fun toString() = values.joinToString(prefix = "[", postfix = "]")

    companion object {
        private val isEmpty = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArrayValue)
            BoolValue(thisValue.values.isEmpty())
        }

        private val size = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArrayValue)
            IntegerValue(thisValue.values.size.toLong())
        }

        private val contains = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is ArrayValue)
            BoolValue(arguments[0] in thisValue.values)
        }

        private val iterator = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArrayValue)
            ArrayIterator(thisValue)
        }

        private val push = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is ArrayValue)
            thisValue.values.add(arguments[0])
            VoidValue
        }

        private val pop = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArrayValue)
            if (thisValue.values.isEmpty()) {
                OptionalValue(null)
            } else OptionalValue(thisValue.values.removeLast())
        }

        private val first = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArrayValue)
            OptionalValue(thisValue.values.firstOrNull())
        }

        private val last = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArrayValue)
            OptionalValue(thisValue.values.lastOrNull())
        }
    }
}

data class ArraySlice(val array: ArrayValue, val range: IntRange) : Value() {
    init {
        this["is_empty"] = isEmpty
        this["contains"] = contains
        this["size"] = size
        this["iterator"] = iterator
        this["to_array"] = toArray
        this["first"] = first
        this["last"] = last
    }

    override fun typeName() = "ArraySlice"

    override fun toString() = "ArraySlice($range)"

    companion object {
        private val isEmpty = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArraySlice)
            BoolValue(thisValue.range.isEmpty() || thisValue.array.values.slice(thisValue.range).isEmpty())
        }

        private val contains = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is ArraySlice)
            BoolValue(arguments[0] in thisValue.array.values.slice(thisValue.range))
        }

        private val size = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArraySlice)

            // Note: this is what Jakt does, not totally sure why
            if (thisValue.range.last > thisValue.array.values.size) {
                IntegerValue(0)
            } else IntegerValue((thisValue.range.last - thisValue.range.first).toLong())
        }

        private val iterator = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArraySlice)
            ArrayIterator(thisValue.array, thisValue.range.first, thisValue.range.last)
        }

        private val toArray = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArraySlice)
            ArrayValue(thisValue.array.values.slice(thisValue.range).toMutableList())
        }

        private val first = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArraySlice)
            thisValue.array.values[thisValue.range.first]
        }

        private val last = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is ArraySlice)
            thisValue.array.values[thisValue.range.last]
        }
    }
}

object StringStruct : Value() {
    private val repeated = BuiltinFunction(2) { thisValue, arguments ->
        require(thisValue == null)
        val (char, count) = arguments
        require(char is CharValue && count is IntegerValue)
        StringValue(buildString {
            repeat(count.value.toInt()) { append(char) }
        })
    }

    private val number = BuiltinFunction(1) { thisValue, arguments ->
        require(thisValue == null)
        val arg = arguments[0]
        require(arg is IntegerValue)
        StringValue(arg.value.toString())
    }

    init {
        this["number"] = number
        this["repeated"] = repeated
    }

    override fun typeName() = "String"

    override fun toString() = "StringStruct"
}

data class StringValue(val value: String) : Value() {
    init {
        this["is_empty"] = isEmpty
        this["length"] = length
        this["hash"] = hash
        this["substring"] = substring
        this["to_uint"] = toUInt
        this["to_int"] = toInt
        this["is_whitespace"] = isWhitespace
        this["contains"] = contains
        this["replace"] = replace
        this["byte_at"] = byteAt
        this["split"] = split
        this["starts_with"] = startsWith
        this["ends_with"] = endsWith
    }

    override fun typeName() = "String"

    override fun toString() = "\"$value\""

    companion object {
        private val whiteSpace = setOf(' ', '\t', '\n', 0xb.toChar() /* \v */, 0xc.toChar() /* \f */, '\r')

        private val isEmpty = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is StringValue)
            BoolValue(thisValue.value.isEmpty())
        }

        private val length = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is StringValue)
            IntegerValue(thisValue.value.length.toLong())
        }

        private val hash = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is StringValue)
            val string = thisValue.value

            // See runtime/Jakt/StringHash.h
            if (string.isEmpty())
                return@BuiltinFunction IntegerValue(0L)

            var hash = string.length

            for (ch in string) {
                hash += ch.code
                hash += hash shl 10
                hash = hash or (hash shr 6)
            }

            hash += hash shl 3
            hash = hash or (hash shr 11)
            hash += hash shl 15

            IntegerValue(hash.toLong())
        }

        private val substring = BuiltinFunction(2) { thisValue, arguments ->
            require(thisValue is StringValue)
            val (start, end) = arguments
            require(start is IntegerValue && end is IntegerValue)
            StringValue(thisValue.value.substring(start.value.toInt(), end.value.toInt()))
        }

        private val toUInt = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is StringValue)
            IntegerValue(thisValue.value.toLong())
        }

        private val toInt = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is StringValue)
            IntegerValue(thisValue.value.toLong())
        }

        private val isWhitespace = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is StringValue)
            BoolValue(thisValue.value.all { it in whiteSpace })
        }

        private val contains = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is StringValue)
            val arg = arguments[0]
            require(arg is StringValue)
            BoolValue(arg.value in thisValue.value)
        }

        private val replace = BuiltinFunction(2) { thisValue, arguments ->
            require(thisValue is StringValue)
            val (replace, with) = arguments
            require(replace is StringValue && with is StringValue)
            StringValue(thisValue.value.replace(replace.value, with.value))
        }

        private val byteAt = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is StringValue)
            val arg = arguments[0]
            require(arg is IntegerValue)
            IntegerValue(thisValue.value.toByteArray()[arg.value.toInt()].toLong())
        }

        private val split = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is StringValue)
            val arg = arguments[0]
            require(arg is CharValue)
            ArrayValue(thisValue.value.split(arg.value).map(::StringValue).toMutableList())
        }

        private val startsWith = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is StringValue)
            val arg = arguments[0]
            require(arg is StringValue)
            BoolValue(thisValue.value.startsWith(arg.value))
        }

        private val endsWith = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is StringValue)
            val arg = arguments[0]
            require(arg is StringValue)
            BoolValue(thisValue.value.endsWith(arg.value))
        }
    }
}

object StringBuilderStruct : Value() {
    private val create = BuiltinFunction(0) { thisValue, arguments ->
        require(thisValue == null)
        require(arguments.isEmpty())
        StringBuilderInstance()
    }

    init {
        this["create"] = create
    }

    override fun typeName() = "StringBuilder"

    override fun toString() = "StringBuilderStruct"
}

class StringBuilderInstance : Value() {
    val builder = StringBuilder()

    init {
        this["append"] = append
        this["append_string"] = appendString
        this["append_code_point"] = appendCodePoint
        this["to_string"] = toString
        this["is_empty"] = isEmpty
        this["length"] = length
        this["clear"] = clear
    }

    override fun typeName() = "StringBuilder"

    override fun toString() = "StringBuilder(\"$builder\")"

    companion object {
        private val append = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is StringBuilderInstance)
            val arg = arguments[0]
            require(arg is ByteCharValue)

            thisValue.builder.appendCodePoint(arg.value.toInt())
            VoidValue
        }

        private val appendString = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is StringBuilderInstance)
            val arg = arguments[0]
            require(arg is StringValue)

            thisValue.builder.append(arg.value)
            VoidValue
        }

        private val appendCodePoint = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is StringBuilderInstance)
            val arg = arguments[0]
            require(arg is IntegerValue)

            thisValue.builder.appendCodePoint(arg.value.toInt())
            VoidValue
        }

        private val toString = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is StringBuilderInstance)
            StringValue(thisValue.builder.toString())
        }

        private val isEmpty = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is StringBuilderInstance)
            BoolValue(thisValue.builder.isEmpty())
        }

        private val length = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is StringBuilderInstance)
            IntegerValue(thisValue.builder.length.toLong())
        }

        private val clear = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is StringBuilderInstance)
            thisValue.builder.clear()
            VoidValue
        }
    }
}

// TODO: no clue if this is works the way it does in Jakt
data class DictionaryIterator(val dictionary: DictionaryValue) : Value() {
    private val remainingKeys = dictionary.elements.keys

    init {
        this["next"] = next
    }

    override fun typeName() = "DictionaryIterator"

    override fun toString() = "DictionaryIterator"

    companion object {
        private val next = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is DictionaryIterator)
            val nextKey = thisValue.remainingKeys.random()
            thisValue.remainingKeys.remove(nextKey)
            TupleValue(mutableListOf(nextKey, thisValue.dictionary.elements[nextKey]!!))
        }
    }
}

data class DictionaryValue(val elements: MutableMap<Value, Value>) : Value() {
    init {
        this["is_empty"] = isEmpty
        this["get"] = get
        this["contains"] = contains
        this["set"] = set
        this["remove"] = remove
        this["clear"] = clear
        this["size"] = size
        this["keys"] = keys
        this["iterator"] = iterator
    }

    override fun typeName() = "Dictionary"

    override fun toString() = elements.entries.joinToString(prefix = "{", postfix = "}") {
        "${it.key}: ${it.value}"
    }

    companion object {
        private val isEmpty = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is DictionaryValue)
            BoolValue(thisValue.elements.isEmpty())
        }

        private val get = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is DictionaryValue)
            require(arguments.size == 1)
            OptionalValue(thisValue.elements[arguments[0]])
        }

        private val contains = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is DictionaryValue)
            BoolValue(arguments[0] in thisValue.elements)
        }

        private val set = BuiltinFunction(2) { thisValue, arguments ->
            require(thisValue is DictionaryValue)
            thisValue.elements[arguments[0]] = arguments[1]
            VoidValue
        }

        private val remove = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is DictionaryValue)
            BoolValue(thisValue.elements.remove(arguments[0]) != null)
        }

        private val clear = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is DictionaryValue)
            thisValue.elements.clear()
            VoidValue
        }

        private val size = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is DictionaryValue)
            IntegerValue(thisValue.elements.size.toLong())
        }

        // TODO: What is the ordering guarantee for this in Jakt?
        private val keys = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is DictionaryValue)
            ArrayValue(thisValue.elements.keys.toMutableList())
        }

        private val iterator = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is DictionaryValue)
            DictionaryIterator(thisValue)
        }
    }
}

data class SetIterator(val values: MutableSet<Value>) : Value() {
    init {
        this["next"] = next
    }

    override fun typeName() = "SetIterator"

    override fun toString() = "SetIterator"

    companion object {
        private val next = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is SetIterator)
            val nextValue = thisValue.values.random()
            thisValue.values.remove(nextValue)
            nextValue
        }
    }
}

data class SetValue(val values: MutableSet<Value>) : Value() {
    init {
        this["is_empty"] = isEmpty
        this["contains"] = contains
        this["add"] = add
        this["remove"] = remove
        this["clear"] = clear
        this["size"] = size
        this["iterator"] = iterator
    }

    override fun typeName() = "Set"

    override fun toString() = values.joinToString(prefix = "{", postfix = "}")

    companion object {
        private val isEmpty = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is SetValue)
            BoolValue(thisValue.values.isEmpty())
        }

        private val contains = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is SetValue)
            BoolValue(arguments[0] in thisValue.values)
        }

        private val add = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is SetValue)
            thisValue.values.add(arguments[0])
            VoidValue
        }

        private val remove = BuiltinFunction(1) { thisValue, arguments ->
            require(thisValue is SetValue)
            BoolValue(thisValue.values.remove(arguments[0]))
        }

        private val clear = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is SetValue)
            thisValue.values.clear()
            VoidValue
        }

        private val size = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is SetValue)
            IntegerValue(thisValue.values.size.toLong())
        }

        private val iterator = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is SetValue)
            SetIterator(thisValue.values.toMutableSet())
        }
    }
}

data class RangeValue(val start: Long, val end: Long, val isInclusive: Boolean) : Value() {
    private var current = start
    private val forwards = start <= end

    val range: IntRange
        get() = if (isInclusive) start.toInt()..end.toInt() else start.toInt() until end.toInt()

    init {
        this["next"] = next
        this["inclusive"] = inclusive
        this["exclusive"] = exclusive
    }

    override fun typeName() = "Range"

    override fun toString() = "Range($start..$end, inclusive = $isInclusive)"

    private fun getAndAdvance(): Long {
        return current.also {
            if (forwards) current++ else current--
        }
    }

    private fun isDone(): Boolean {
        return when {
            forwards && isInclusive -> current > end
            forwards -> current >= end
            isInclusive -> current < start
            else -> current <= start
        }
    }

    companion object {
        private val next = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is RangeValue)
            if (thisValue.isDone()) {
                OptionalValue(null)
            } else OptionalValue(IntegerValue(thisValue.getAndAdvance()))
        }

        private val inclusive = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is RangeValue)
            thisValue.copy(isInclusive = true)
        }

        private val exclusive = BuiltinFunction(0) { thisValue, _ ->
            require(thisValue is RangeValue)
            thisValue.copy(isInclusive = false)
        }
    }
}

object ErrorStruct : Value() {
    private val fromErrno = BuiltinFunction(1) { thisValue, arguments ->
        require(thisValue == null)
        val arg = arguments[0]
        require(arg is IntegerValue)
        ErrorInstance(arg.value)
    }

    init {
        this["from_errno"] = fromErrno
    }

    override fun typeName() = "Error"

    override fun toString() = "ErrorStruct"
}

class ErrorInstance(private val codeValue: Long) : Value() {
    override fun typeName() = "Error"

    override fun toString() = "Error(code = $codeValue)"
}

object FileStruct : Value() {
    private val exists = BuiltinFunction(1) { thisValue, arguments ->
        require(thisValue == null)
        val arg = arguments[0]
        require(arg is StringValue)
        BoolValue(File(arg.value).exists())
    }

    private val openForReading = BuiltinFunction(1) { thisValue, arguments ->
        require(thisValue == null)
        val arg = arguments[0]
        require(arg is StringValue)
        FileInstance(File(arg.value))
    }

    init {
        this["exists"] = exists
        this["open_for_reading"] = openForReading
    }

    override fun typeName() = "File"

    override fun toString() = "FileStruct"
}

class FileInstance(val file: File) : Value() {
    init {
        this["read_all"] = readAll
    }

    override fun typeName() = "File"

    override fun toString() = "File($file)"

    companion object {
        private val readAll = BuiltinFunction(0) { thisValue, arguments ->
            require(thisValue is FileInstance)
            require(arguments.isEmpty())
            StringValue(thisValue.file.readText())
        }
    }
}

// Free functions

val jaktGetTargetTripleStringFunction = BuiltinFunction(0) { _, _ ->
    JaktProjectListener.targetTriple.get() ?: StringValue("unknown-unknown-unknown-unknown")
}

val abortFunction = BuiltinFunction(0) { _, _ -> error("aborted") }

// TODO: saturated/truncated functions when we have generic information

// TODO: Format functions, not trivial since Kotlin does not support the
//       Serenity/Python-style format arguments
