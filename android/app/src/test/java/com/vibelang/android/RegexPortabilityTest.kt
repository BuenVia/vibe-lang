package com.vibelang.android

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Unit tests run on the desktop JVM, whose regex engine accepts a bare "}" (e.g. `\{\{(.*)}}`). Android's ICU
 * engine rejects it with a PatternSyntaxException, which crashed the app when Cloze was first used. This test
 * scans the app's regex literals for a "}" that is neither escaped nor closing a `{n,m}` quantifier or a
 * `\p{…}` class, so the mistake is caught before it reaches a phone.
 */
class RegexPortabilityTest {

    private val literal = Regex("Regex\\(\"\"\"(.*?)\"\"\"\\)|Regex\\(\"((?:[^\"\\\\]|\\\\.)*)\"\\)")

    @Test
    fun `regex literals have no unescaped closing brace`() {
        val sources = File("src/main").walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("no sources found from ${File(".").absolutePath}", sources.isNotEmpty())
        val problems = sources.flatMap { file ->
            literal.findAll(file.readText()).mapNotNull { m ->
                val pattern = m.groupValues[1].ifEmpty { m.groupValues[2] }
                if (hasBareClosingBrace(pattern)) "${file.name}: $pattern" else null
            }.toList()
        }
        assertTrue("Escape '}' as '\\}' for Android:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `the checker itself spots the bug`() {
        assertTrue(hasBareClosingBrace("""\{\{([^{}]*)}}"""))
        assertTrue(!hasBareClosingBrace("""\{\{([^{}]*)\}\}"""))
        assertTrue(!hasBareClosingBrace("""a{2,3}\p{L}[^{}]"""))
    }

    /** True when a "}" appears outside a character class without a matching "{" quantifier or \p{…}. */
    private fun hasBareClosingBrace(pattern: String): Boolean {
        var i = 0
        var inClass = false
        var openBraces = 0
        while (i < pattern.length) {
            val c = pattern[i]
            when {
                c == '\\' -> {
                    // \p{…} / \P{…} open a brace that a later "}" may close.
                    if (i + 2 < pattern.length && (pattern[i + 1] == 'p' || pattern[i + 1] == 'P') && pattern[i + 2] == '{') {
                        openBraces++
                        i += 3
                        continue
                    }
                    i += 2
                    continue
                }
                inClass -> if (c == ']') inClass = false
                c == '[' -> inClass = true
                c == '{' -> openBraces++
                c == '}' -> if (openBraces == 0) return true else openBraces--
            }
            i++
        }
        return false
    }
}
