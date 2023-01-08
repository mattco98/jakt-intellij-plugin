package org.serenityos.jakt.comptime

import com.intellij.openapi.util.Ref
import org.serenityos.jakt.utils.unreachable

// Based on https://github.com/SerenityOS/serenity/blob/master/AK/Format.cpp

data class FormatString(
    private val literals: List<String>,
    private val specifierStrings: List<String>,
) {
    init {
        require(specifierStrings.size == literals.size - 1)
    }

    fun apply(arguments: List<Value>): String {
        val context = Context(arguments)
        val specifiers = specifierStrings.map { FormatSpecifierParser(it).parse(context) }

        return buildString {
            append(literals[0])

            for (i in specifiers.indices) {
                specifiers[i].apply(this, arguments)
                append(literals[i + 1])
            }
        }
    }
}

data class Specifier(
    var index: Int = 0,
    var alignment: Alignment = Alignment.Right,
    var sign: Sign = Sign.OnlyIfNeeded,
    var mode: Mode = Mode.Default,
    var alternative: Boolean = false,
    var fillChar: Char = ' ',
    var zeroPad: Boolean = false,
    var width: Int? = null,
    var precision: Int? = null,
) {
    enum class Alignment {
        Left,
        Center,
        Right,
    }

    enum class Sign {
        OnlyIfNeeded,
        Always,
        Reserved,
    }

    enum class Mode(val str: kotlin.String) {
        Default(""),
        Binary("b"),
        BinaryUppercase("B"),
        Decimal("d"),
        Octal("o"),
        Hexadecimal("x"),
        HexadecimalUppercase("X"),
        Character("c"),
        String("s"),
        Pointer("p"),
        Float("f"),
        HexFloat("a"),
        HexFloatUppercase("A"),
        HexDump("hex-dump"),
    }

    // TODO: This needs a lot of work to be accurate
    fun apply(builder: StringBuilder, arguments: List<Value>) {
        val target = arguments.getOrNull(index) ?:
            error("Format specifier refers to argument ${index + 1}, but only ${arguments.size} arguments were provided")

        check(target is PrimitiveValue) { "Cannot format non-primitive type ${target.typeName()} at comptime" }

        check(mode != Mode.Pointer && mode != Mode.HexDump && mode != Mode.Binary && mode != Mode.BinaryUppercase) {
            "Unsupported format specifier '${mode.str}'"
        }

        check(alignment != Alignment.Center) {
            "Unsupported alignment '^'"
        }

        check(fillChar == ' ') {
            "Unsupported non-space fill character"
        }

        val javaFormatSpecifier = buildString {
            append('%')

            if (alignment == Alignment.Left)
                append('-')

            if (alternative)
                append('#')

            when (sign) {
                Sign.Always -> append('+')
                Sign.Reserved -> append(' ')
                Sign.OnlyIfNeeded -> {}
            }

            if (zeroPad)
                append('0')

            if (width != null)
                append(width)

            if (precision != null) {
                append('.')
                append(precision)
            }

            if (mode == Mode.Default) {
                mode = when (target) {
                    is StringValue -> Mode.String
                    is IntegerValue -> Mode.Decimal
                    is FloatValue -> Mode.Float
                    is CharValue -> Mode.Character
                    else -> unreachable()
                }
            }

            append(mode.str)
        }

        builder.append(javaFormatSpecifier.format(target.value))
    }
}

open class GenericParser(val text: String) {
    var cursor = 0

    val done: Boolean
        get() = cursor > text.lastIndex

    val char: Char
        get() = text[cursor]

    fun consumeNumber(): Int? {
        val pos = cursor

        while (!done && char.isDigit())
            cursor++

        if (pos == cursor)
            return null

        return text.substring(pos, cursor).toIntOrNull() ?: run {
            cursor = pos
            null
        }
    }

    fun consumeIf(char: Char): Boolean {
        return if (matches(char)) {
            cursor++
            true
        } else false
    }

    fun consumeIf(str: String): Boolean {
        return if (matches(str)) {
            cursor += str.length
            true
        } else false
    }

    fun peek(n: Int = 0) = text.getOrNull(cursor + n)

    fun consume() = char.also { cursor++ }

    fun matches(ch: Char): Boolean {
        return !done && ch == char
    }

    fun matches(string: String): Boolean {
        return text.substring(cursor).startsWith(string)
    }
}

class Context(val arguments: List<Value>, private var nextValue: Int = 0) {
    fun nextIndex() = nextValue++
}

class FormatSpecifierParser(specifier: String) : GenericParser(specifier) {
    fun parse(context: Context): Specifier = with(Specifier()) {
        index = consumeNumber() ?: context.nextIndex()

        if (!consumeIf(':'))
            return@with this

        if ("<^>".contains(peek(1) ?: 'a')) {
            check(char !in "{}") { "Malformed specifier \"$text\"" }
            fillChar = consume()
        }

        alignment = when {
            consumeIf('<') -> Specifier.Alignment.Left
            consumeIf('^') -> Specifier.Alignment.Center
            consumeIf('>') -> Specifier.Alignment.Right
            else -> alignment
        }

        sign = when {
            consumeIf('-') -> Specifier.Sign.OnlyIfNeeded
            consumeIf('+') -> Specifier.Sign.Always
            consumeIf(' ') -> Specifier.Sign.Reserved
            else -> sign
        }

        if (consumeIf('#'))
            alternative = true

        if (consumeIf('0'))
            zeroPad = true

        val index = Ref<Int?>(null)
        if (consumeReplacementField(index)) {
            if (index.isNull)
                index.set(context.nextIndex())

            val widthValue = context.arguments.getOrNull(index.get()!!)
            check(widthValue != null) {
                "Width parameter refers to non-existent argument ${index.get()!!}"
            }
            check(widthValue is IntegerValue) {
                "Expected integer for width argument at index ${index.get()!!}, found ${widthValue.typeName()}"
            }
            width = widthValue.value.toInt()
        } else {
            val num = consumeNumber()
            if (num != null)
                width = num
        }

        if (consumeIf('.')) {
            if (consumeReplacementField(index)) {
                if (index.isNull)
                    index.set(context.nextIndex())

                val precisionValue = context.arguments.getOrNull(index.get()!!)
                check(precisionValue != null) {
                    "Precision parameter refers to non-existent argument ${index.get()!!}"
                }
                check(precisionValue is IntegerValue) {
                    "Expected integer for precision argument at index ${index.get()!!}, found ${precisionValue.typeName()}"
                }
                precision = precisionValue.value.toInt()
            } else {
                val num = consumeNumber()
                if (num != null)
                    precision = num
            }
        }

        mode = when {
            consumeIf('b') -> Specifier.Mode.Binary
            consumeIf('B') -> Specifier.Mode.BinaryUppercase
            consumeIf('d') -> Specifier.Mode.Decimal
            consumeIf('o') -> Specifier.Mode.Octal
            consumeIf('x') -> Specifier.Mode.Hexadecimal
            consumeIf('X') -> Specifier.Mode.HexadecimalUppercase
            consumeIf('c') -> Specifier.Mode.Character
            consumeIf('s') -> Specifier.Mode.String
            consumeIf('P') -> Specifier.Mode.Pointer
            consumeIf('f') -> Specifier.Mode.Float
            consumeIf('a') -> Specifier.Mode.HexFloat
            consumeIf('A') -> Specifier.Mode.HexFloatUppercase
            consumeIf("hex-dump") -> Specifier.Mode.HexDump
            matches('}') -> Specifier.Mode.Default
            !done -> error("Unknown format specifier '$char'")
            else -> mode
        }

        check(consumeIf('}'))

        check(done)

        this
    }

    private fun consumeReplacementField(ref: Ref<Int?>): Boolean {
        if (!consumeIf('{'))
            return false

        ref.set(consumeNumber())

        check(consumeIf('}'))

        return true
    }
}

class FormatStringParser(formatString: String) : GenericParser(formatString) {
    fun parse(): FormatString {
        if (done)
            return FormatString(listOf(""), emptyList())

        val literals = mutableListOf<String>()
        val specifiers = mutableListOf<String>()

        while (true) {
            literals.add(consumeLiteral())
            val specifier = consumeSpecifier()

            if (specifier == null) {
                check(done) {
                    "Expected specifier at offset $cursor"
                }

                return FormatString(literals, specifiers)
            }

            specifiers.add(specifier)
        }
    }

    private fun consumeLiteral(): String {
        val pos = cursor

        while (!done) {
            if (consumeIf("{{"))
                continue

            if (consumeIf("}}"))
                continue

            if (matches("{") || matches("}"))
                return text.substring(pos, cursor)

            cursor++
        }

        return text.substring(pos)
    }

    private fun consumeSpecifier(): String? {
        require(!matches("}")) {
            "Unexpected '}' at offset $cursor"
        }

        if (!consumeIf("{"))
            return null

        val pos = cursor

        consumeNumber()

        if (consumeIf(":")) {
            var level = 1

            while (level > 0) {
                check(!done) {
                    "Unexpected end of string in format specifier"
                }

                if (matches("{"))
                    level++

                if (matches("}"))
                    level--

                cursor++
            }

            return text.substring(pos, cursor)
        }

        check(consumeIf("}")) {
            "Expected '}' at offset $cursor"
        }

        return ""
    }
}
