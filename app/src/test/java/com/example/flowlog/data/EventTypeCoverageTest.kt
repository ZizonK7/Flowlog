package com.example.flowlog.data

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Every [com.example.flowlog.data.constants.EventType] constant must be written somewhere:
 * its `EventType.<NAME>` has to appear in a main-source Kotlin file other than EventType.kt
 * (comments excluded). A constant with no code path yet goes in [noPathYet] with the reason,
 * so the gap stays visible instead of a name nobody logs.
 *
 * Reads the source files (no Android classes). Gradle runs unit tests from the module
 * directory (`app/`); running from the repository root also works.
 */
class EventTypeCoverageTest {

    private val noPathYet = mapOf(
        // Room migrations run inside the first database open; event_logs cannot be written
        // before or during one. MIGRATION_COMPLETED is recorded after the open instead
        // (FlowlogDatabase.recordMigrationIfVersionChanged).
        "MIGRATION_STARTED" to "no write possible before or during a Room migration",
        // A failed migration leaves the database unusable, and the exception usually ends
        // the process at the app's first query, so there is no reliable moment to write it.
        "MIGRATION_FAILED" to "database unusable after a failed migration"
    )

    @Test
    fun everyEventTypeIsLoggedSomewhereOrListedWithAReason() {
        val missing = eventTypeNames() - usedNames() - noPathYet.keys
        if (missing.isNotEmpty()) {
            fail(
                "EventType constants never used outside EventType.kt: ${missing.sorted()}. " +
                    "Log them where the action happens, or add them to noPathYet with the reason."
            )
        }
    }

    @Test
    fun noPathYetOnlyHoldsRealConstantsThatAreStillUnused() {
        val names = eventTypeNames()
        val unknown = noPathYet.keys - names
        assertTrue("noPathYet names not in EventType: $unknown", unknown.isEmpty())
        val nowUsed = noPathYet.keys.intersect(usedNames())
        assertTrue("now logged, remove from noPathYet: $nowUsed", nowUsed.isEmpty())
    }

    @Test
    fun eventTypeFileIsParsed() {
        assertTrue("expected EventType constants, found ${eventTypeNames().size}", eventTypeNames().size >= 30)
    }

    private fun mainSourceRoot(): File {
        val candidates = listOf(File("src/main/java"), File("app/src/main/java"))
        return candidates.firstOrNull { File(it, EVENT_TYPE_PATH).isFile }
            ?: error("main sources not found from ${File(".").absolutePath}; tried $candidates")
    }

    private fun eventTypeFile(): File = File(mainSourceRoot(), EVENT_TYPE_PATH)

    private fun eventTypeNames(): Set<String> =
        CONST_REGEX.findAll(stripComments(eventTypeFile().readText()))
            .map { it.groupValues[1] }
            .toSet()

    private fun usedNames(): Set<String> {
        val eventTypeFile = eventTypeFile().canonicalFile
        return mainSourceRoot().walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.canonicalFile != eventTypeFile }
            .flatMap { file -> USE_REGEX.findAll(stripComments(file.readText())).map { it.groupValues[1] } }
            .toSet()
    }

    private fun stripComments(source: String): String =
        source.replace(BLOCK_COMMENT, " ").replace(LINE_COMMENT, "")

    private companion object {
        const val EVENT_TYPE_PATH = "com/example/flowlog/data/constants/EventType.kt"
        val CONST_REGEX = Regex("""const\s+val\s+([A-Z][A-Z0-9_]*)\s*=""")
        val USE_REGEX = Regex("""\bEventType\.([A-Z][A-Z0-9_]*)\b""")
        val BLOCK_COMMENT = Regex("""/\*[\s\S]*?\*/""")
        val LINE_COMMENT = Regex("""//[^\n]*""")
    }
}
