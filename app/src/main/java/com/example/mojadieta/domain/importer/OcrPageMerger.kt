package com.example.mojadieta.domain.importer

/**
 * Łączy tekst z kilku zrzutów ekranu tego samego przepisu.
 * Kolejne zrzuty zwykle częściowo się pokrywają (przewinięta strona), więc
 * powtórzony fragment na styku dwóch zrzutów usuwamy, aby nie dublować składników ani nagłówków.
 */
object OcrPageMerger {

    fun merge(pages: List<String>): String {
        var result = mutableListOf<String>()
        pages.forEach { page ->
            val lines = page.lines().map { it.trim() }.filter { it.isNotEmpty() }
            result = joinOverlapping(result, lines).toMutableList()
        }
        return result.joinToString("\n")
    }

    /** Dokleja [next] do [prev], pomijając wspólny fragment na styku. */
    fun joinOverlapping(prev: List<String>, next: List<String>): List<String> {
        if (prev.isEmpty()) return next
        if (next.isEmpty()) return prev
        // Porównujemy tylko linie z treścią – drobne elementy interfejsu („+”, „1”, „v”) OCR odczytuje niekonsekwentnie.
        val aIdx = prev.indices.filter { significant(prev[it]) }
        val bIdx = next.indices.filter { significant(next[it]) }
        val a = aIdx.map { key(prev[it]) }
        val b = bIdx.map { key(next[it]) }
        if (a.isEmpty() || b.isEmpty()) return prev + next
        // Szukamy najdłuższego pokrycia: początek następnego zrzutu = końcówka poprzedniego.
        // Ostatnie 1–2 linie poprzedniego zrzutu mogą być ucięte lub wyblakłe, więc dopuszczamy ich brak dopasowania.
        val from = maxOf(0, a.size - MAX_OVERLAP)
        for (p in from until a.size) {
            var m = 0
            while (p + m < a.size && m < b.size && similar(a[p + m], b[m])) m++
            val reachesEnd = p + m >= a.size - 2
            val meaningful = m >= 2 || (m == 1 && a[p].length >= 15 && p + m == a.size)
            if (reachesEnd && meaningful) {
                // Odcinamy końcówkę poprzedniego zrzutu od pierwszej wspólnej linii; resztę bierzemy z następnego.
                return prev.subList(0, aIdx[p]) + next
            }
        }
        return prev + next
    }

    private fun significant(line: String): Boolean = key(line).let { k -> k.length >= 3 && k.any { it.isLetter() } }

    private fun key(line: String) = RecipeTextParser.normalize(line).replace(Regex("[^a-z0-9]"), "")

    private fun similar(x: String, y: String): Boolean {
        if (x.isEmpty() || y.isEmpty()) return x == y
        if (x == y) return true
        val shorter = minOf(x.length, y.length)
        if (shorter >= 6 && (x.startsWith(y) || y.startsWith(x))) return true
        if (shorter < 6) return false
        return distance(x, y) <= maxOf(1, maxOf(x.length, y.length) / 8)
    }

    private fun distance(s: String, t: String): Int {
        var prevRow = IntArray(t.length + 1) { it }
        for (i in 1..s.length) {
            val row = IntArray(t.length + 1)
            row[0] = i
            for (j in 1..t.length) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                row[j] = minOf(row[j - 1] + 1, prevRow[j] + 1, prevRow[j - 1] + cost)
            }
            prevRow = row
        }
        return prevRow[t.length]
    }

    private const val MAX_OVERLAP = 60
}
