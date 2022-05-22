package org.serenityos.jakt

import com.intellij.testFramework.ParsingTestCase

class JaktParserTest : ParsingTestCase("", "jakt", JaktParserDefinition()) {
    override fun getTestDataPath(): String {
        return "src/main/resources/testData"
    }

    fun testParsingTestData() = doTest(true)

    override fun skipSpaces() = true

    override fun includeRanges() = true
}