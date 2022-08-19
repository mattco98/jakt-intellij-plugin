package org.serenityos.jakt.utils

interface BitMask {
    val ordinal: Int

    fun bitMask() = 1 shl ordinal

    infix fun or(other: Int) = bitMask() or other
    infix fun or(other: BitMask) = bitMask() or other.bitMask()

    infix fun and(other: Int) = bitMask() and other
    infix fun and(other: BitMask) = bitMask() and other.bitMask()

    companion object {
        fun makeMask(vararg pairs: Pair<BitMask, Boolean>): Int {
            return pairs.filter { it.second }
                .map { it.first.bitMask() }
                .fold(0) { a, b -> a or b }
        }
    }
}

fun Int.isSet(mask: BitMask) = (this and mask.bitMask()) != 0
