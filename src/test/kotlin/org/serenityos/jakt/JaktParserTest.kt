package org.serenityos.jakt

import com.intellij.testFramework.ParsingTestCase

/**
 * Requires a file at src/main/resources/testData/ParsingTestData.jakt with
 * code to be ran through the parser.
 */
class JaktParserTest : ParsingTestCase("", "jakt", JaktParserDefinition()) {
    override fun getTestDataPath(): String {
        return "src/main/resources/testData"
    }

    fun testParsingTestData() = doTest(true)

    override fun skipSpaces() = true

    override fun includeRanges() = true
}