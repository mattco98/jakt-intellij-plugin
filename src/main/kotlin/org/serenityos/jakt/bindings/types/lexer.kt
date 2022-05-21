package org.serenityos.jakt.bindings.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Span(@SerialName("file_id") val fileId: Int, val start: Int, val end: Int)

@Serializable(with = TokenContents.Serializer::class)
sealed class TokenContents {
    object Serializer : KSerializer<TokenContents> by rustEnumSerializer()

    data class SingleQuotedString(val value: String) : TokenContents()

    data class QuotedString(val value: String) : TokenContents()

    data class Number(val value: NumericConstant) : TokenContents()

    data class Name(val value: String) : TokenContents()

    object Semicolon : TokenContents()

    object Colon : TokenContents()

    object ColonColon : TokenContents()

    object LParen : TokenContents()

    object RParen : TokenContents()

    object LCurly : TokenContents()

    object RCurly : TokenContents()

    object LSquare : TokenContents()

    object RSquare : TokenContents()

    object PercentSign : TokenContents()

    object Plus : TokenContents()

    object Minus : TokenContents()

    object Equal : TokenContents()

    object PlusEqual : TokenContents()

    object PlusPlus : TokenContents()

    object MinusEqual : TokenContents()

    object MinusMinus : TokenContents()

    object AsteriskEqual : TokenContents()

    object ForwardSlashEqual : TokenContents()

    object PercentSignEqual : TokenContents()

    object NotEqual : TokenContents()

    object DoubleEqual : TokenContents()

    object GreaterThan : TokenContents()

    object GreaterThanOrEqual : TokenContents()

    object LessThan : TokenContents()

    object LessThanOrEqual : TokenContents()

    object LeftArithmeticShift : TokenContents()

    object LeftShift : TokenContents()

    object LeftShiftEqual : TokenContents()

    object RightShift : TokenContents()

    object RightArithmeticShift : TokenContents()

    object RightShiftEqual : TokenContents()

    object Asterisk : TokenContents()

    object Ampersand : TokenContents()

    object AmpersandEqual : TokenContents()

    object Pipe : TokenContents()

    object PipeEqual : TokenContents()

    object Caret : TokenContents()

    object CaretEqual : TokenContents()

    object Tilde : TokenContents()

    object ForwardSlash : TokenContents()

    object ExclamationPoint : TokenContents()

    object QuestionMark : TokenContents()

    object QuestionMarkQuestionMark : TokenContents()

    object Comma : TokenContents()

    object Dot : TokenContents()

    object DotDot : TokenContents()

    object Eol : TokenContents()

    object Eof : TokenContents()

    object FatArrow : TokenContents()

    object Garbage : TokenContents()
}

@Serializable
data class Token(val contents: TokenContents, val span: Span)
