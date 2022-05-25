package org.serenityos.jakt

import com.intellij.testFramework.ParsingTestCase
import java.io.File

/**
 * Requires a file at src/main/resources/testData/ParsingTestData.jakt with
 * code to be ran through the parser.
 */
class JaktParserTest : ParsingTestCase("", "jakt", JaktParserDefinition()) {
    init {
        File("src/main/resources/testData/ParsingTestData.txt").delete()
    }

    override fun getTestDataPath(): String {
        return "src/main/resources/testData"
    }

    fun testParsingTestData() = doTest(true)

    override fun skipSpaces() = true

    override fun includeRanges() = true
}