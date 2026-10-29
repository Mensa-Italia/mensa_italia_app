package it.mensa.shared

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Nessuna funzione visibile da Swift ritorna un `kotlin.Result`.
 *
 * Il bridge di Kotlin/Native lo consegna come un `id` opaco: un fallimento
 * dentro il `Result` non diventa un errore Swift, il `try await` torna normale
 * e il `catch` non scatta. E' successo con `AuthRepository.login` (password
 * sbagliata, nessun messaggio su iOS). Fallimenti come eccezione con
 * `@Throws(Exception::class)`, oppure uno stato dedicato.
 *
 * Il controllo e' sul sorgente perche' sulla JVM un `Result` di una suspend fun
 * non lascia traccia nella firma.
 */
class SwiftBridgeContractTest {

    /** `): Result<…>`, anche qualificato, o il tipo dedotto da `= runCatching`. */
    private val resultReturn = Regex("""\)\s*(:\s*(kotlin\.)?Result<|=\s*runCatching\b)""")
    private val declaration = Regex("""\bfun\b""")
    private val hidden = Regex("""\b(private|internal)\b""")
    private val comment = Regex("""^\s*(//|\*|/\*)""")

    private fun code(line: String) = line.substringBefore("//")

    @Test
    fun noSwiftVisibleFunctionReturnsResult() {
        val roots = listOf("src/commonMain/kotlin", "src/appleMain/kotlin").map(::File)
        assertTrue(roots.first().isDirectory, "il test va lanciato dalla cartella del modulo shared")

        val offenders = roots.filter(File::isDirectory)
            .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.toList() }
            .flatMap { file ->
                val lines = file.readLines()
                lines.indices
                    .filter { !comment.containsMatchIn(lines[it]) && resultReturn.containsMatchIn(code(lines[it])) }
                    .filter { i ->
                        // La riga con `fun`: la firma puo' andare a capo fra i parametri.
                        val start = (i downTo 0).firstOrNull { declaration.containsMatchIn(code(lines[it])) } ?: i
                        !hidden.containsMatchIn(code(lines[start]))
                    }
                    .map { "${file.path}:${it + 1}: ${lines[it].trim()}" }
            }

        assertTrue(offenders.isEmpty(), "Result in una firma visibile da Swift:\n" + offenders.joinToString("\n"))
    }
}
