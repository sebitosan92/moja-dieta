package com.example.mojadieta.domain.importer

import com.example.mojadieta.domain.model.FoodProduct
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.RecipeCategory
import com.example.mojadieta.domain.model.RecipeIngredient
import java.text.Normalizer

/** Przepis odczytany z tekstu (zdjęcie, zrzut ekranu, PDF). */
data class ImportedRecipe(
    val title: String,
    val category: RecipeCategory,
    val mealType: MealType?,
    /** Numer dnia jadłospisu (1, 2, …) albo null. */
    val dayIndex: Int?,
    /** Składniki dopasowane do bazy produktów, z gramaturą. */
    val ingredients: List<RecipeIngredient>,
    /** Składniki, których nie udało się dopasować lub przeliczyć na gramy. */
    val otherIngredients: List<String>,
    val steps: List<String>,
    val calories: Int?,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val servings: Int
) {
    val hasExplicitNutrition: Boolean get() = calories != null
}

data class ImportResult(val recipes: List<ImportedRecipe>, val isMealPlan: Boolean)

/**
 * Heurystyczny parser przepisów z polskiego tekstu.
 * Obsługuje: pojedynczy przepis, e-book z wieloma przepisami (nagłówki „Składniki”/„Przygotowanie”)
 * oraz jadłospis (dni + pory posiłków).
 */
class RecipeTextParser(products: List<FoodProduct>) {

    private val catalog: List<Pair<FoodProduct, List<String>>> =
        products.map { it to tokens(it.name.replace(Regex("\\(.*?\\)"), " ")) }.filter { it.second.isNotEmpty() }

    private enum class Kind { INGREDIENTS, STEPS, DAY, MEAL, SECTION, OTHER }

    private data class Line(val raw: String, val norm: String, val kind: Kind, val meal: MealType? = null, val mealRest: String = "")

    fun parse(text: String): ImportResult {
        val lines = preprocess(text.lines())
            .filter { !isPageNoise(it) }
            .map { classify(it) }
        if (lines.isEmpty()) return ImportResult(emptyList(), false)

        val isMealPlan = lines.any { it.kind == Kind.DAY }
        val ingredientHeaders = lines.indices.filter { lines[it].kind == Kind.INGREDIENTS }

        // Początki przepisów (indeksy linii): tytuły nad „Składnikami” oraz nagłówki posiłków.
        // Dokument może łączyć oba układy (np. e-book z przepisami + jadłospis).
        val headerStarts = ingredientHeaders.mapIndexed { n, h ->
            val lowerBound = if (n == 0) 0 else ingredientHeaders[n - 1] + 1
            findTitleIndex(lines, h, lowerBound)
        }
        val mealStarts = lines.indices.filter { lines[it].kind == Kind.MEAL }
        val starts: List<Int> = (headerStarts + mealStarts).ifEmpty { listOf(0) }.distinct().sorted()
        // Granice przepisów: początki kolejnych przepisów, nagłówki dni i tytuły sekcji (np. „Jadłospis”).
        val boundaries = (starts + lines.indices.filter { lines[it].kind == Kind.DAY || lines[it].kind == Kind.SECTION })
            .distinct().sorted()

        val recipes = starts.mapIndexedNotNull { n, start ->
            val end = boundaries.firstOrNull { it > start } ?: lines.size
            // Jeśli tuż przed przepisem są nagłówki dnia/pory, uwzględniamy je jako kontekst.
            val context = lines.subList(0, start)
            val day = dayIndexAt(lines, start)
            val meal = lines.subList(0, start + 1).lastOrNull { it.kind == Kind.MEAL || it.kind == Kind.DAY }
                ?.takeIf { it.kind == Kind.MEAL }?.meal
            buildRecipe(lines.subList(start, end), day.takeIf { isMealPlan }, meal, context.isEmpty())
        }
        return ImportResult(recipes, isMealPlan)
    }

    // ---------- Porządkowanie tekstu z OCR ----------

    /**
     * Czyści tekst odczytany ze zrzutów ekranu aplikacji i stron z przepisami:
     * usuwa elementy interfejsu, skleja wartości odżywcze zapisane w kolumnach
     * („599 / Kalorie”) i ilości zapisane w osobnej linii pod nazwą składnika.
     */
    fun preprocess(rawLines: List<String>): List<String> {
        var lines = rawLines.map { it.replace('\u00A0', ' ').replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() && !isUiNoise(it) }
        lines = resolveServingsQuestion(lines)
        lines = dropTimeCards(lines)
        lines = mergeNutritionColumns(lines)
        lines = mergeQuantityLines(lines)
        return lines
    }

    private fun isUiNoise(line: String): Boolean {
        val n = normalize(line)
        if (!n.any { it.isLetterOrDigit() }) return true
        if (n.length <= 2 && n.none { it.isDigit() }) return true
        if (TIME_OF_DAY.matches(n)) return true
        if (STATUS_BAR.containsMatchIn(n)) return true
        return UI_NOISE.any { it.containsMatchIn(n) }
    }

    /** „Ile porcji chcesz zrobić? − 1 +” → „Porcje: 1”. */
    private fun resolveServingsQuestion(lines: List<String>): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val n = normalize(lines[i])
            if (SERVINGS_QUESTION.containsMatchIn(n)) {
                val inline = Regex("\\?\\s*(\\d{1,2})\\s*$").find(n)?.groupValues?.get(1)?.toIntOrNull()
                var count = inline
                var skip = 0
                if (count == null) {
                    for (k in 1..3) {
                        val next = lines.getOrNull(i + k) ?: break
                        val v = next.trim().toIntOrNull()
                        if (v != null && v in 1..30) {
                            count = v
                            skip = k
                            break
                        }
                    }
                }
                if (count != null) out += "Porcje: $count"
                i += 1 + skip
                continue
            }
            out += lines[i]
            i++
        }
        return out
    }

    /** Usuwa kafelki typu „3 min / Przygotowanie”, „3 min / Łączny czas”. */
    private fun dropTimeCards(lines: List<String>): List<String> {
        val norms = lines.map { normalize(it).trimEnd(':', '.', ' ') }
        val timeIdx = norms.indices.filter { TIME_ONLY.matches(norms[it]) }.toSet()
        return lines.filterIndexed { i, _ ->
            val n = norms[i]
            when {
                i in timeIdx -> false
                TIME_WITH_LABEL.matches(n) -> false
                n in TIME_LABELS && (i - 2..i + 2).any { it in timeIdx } -> false
                n.startsWith("laczny czas") || n.startsWith("czas calkowity") -> false
                else -> true
            }
        }
    }

    /**
     * Wartości odżywcze w kolumnach: liczby, a pod nimi podpisy („599 33 68 g 24” / „Kalorie Białko Węglo. Tłuszcze”)
     * albo na przemian („599”, „Kalorie”, „33”, „Białko”…). Zamieniamy je na linie zrozumiałe dla parsera.
     */
    private fun mergeNutritionColumns(lines: List<String>): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val labels = macroLabels(lines[i])
            if (labels != null) {
                // Ciąg linii z samymi podpisami…
                val allLabels = labels.toMutableList()
                var j = i + 1
                while (j < lines.size) {
                    val more = macroLabels(lines[j]) ?: break
                    allLabels += more
                    j++
                }
                // …i bezpośrednio przed nim ciąg linii z samymi liczbami.
                val values = mutableListOf<String>()
                var k = out.size
                while (k > 0) {
                    val v = macroValues(out[k - 1]) ?: break
                    values.addAll(0, v)
                    k--
                }
                if (values.size >= allLabels.size) {
                    // Kolumny czytamy od lewej, więc liczby i podpisy łączymy w tej samej kolejności.
                    val used = values.take(allLabels.size)
                    while (out.size > k) out.removeAt(out.lastIndex)
                    allLabels.zip(used).forEach { (label, value) -> out += macroLine(label, value) }
                    i = j
                    continue
                }
                i = j
                continue
            }
            out += lines[i]
            i++
        }
        return out
    }

    private fun macroLabels(line: String): List<String>? {
        val words = normalize(line).replace(Regex("[^a-z ]"), " ").split(' ').filter { it.isNotBlank() }
        if (words.isEmpty() || words.size > 6 || line.any { it.isDigit() }) return null
        val labels = words.map { w ->
            when {
                w.startsWith("kalori") || w == "kcal" || w.startsWith("energi") -> "kcal"
                w.startsWith("bial") || w.startsWith("biat") -> "b"
                w.startsWith("wegl") -> "w"
                w.startsWith("tlusz") || w.startsWith("ttusz") || w.startsWith("tusz") -> "t"
                else -> return null
            }
        }
        return labels
    }

    private fun macroValues(line: String): List<String>? {
        val parts = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:g|kcal)?").findAll(line).map { it.groupValues[1] }.toList()
        if (parts.isEmpty()) return null
        if (!line.matches(Regex("^\\s*(\\d+(?:[.,]\\d+)?\\s*(g|kcal)?\\s*)+$", RegexOption.IGNORE_CASE))) return null
        return parts
    }

    private fun macroLine(label: String, value: String): String = when (label) {
        "kcal" -> "$value kcal"
        "b" -> "Białko $value g"
        "w" -> "Węglowodany $value g"
        else -> "Tłuszcze $value g"
    }

    /** „Banan” + „1,5 x sztuka (180 g)” → „Banan 1,5 x sztuka (180 g)”. */
    private fun mergeQuantityLines(lines: List<String>): List<String> {
        val out = mutableListOf<String>()
        for (line in lines) {
            val prev = out.lastOrNull()
            if (prev != null && QTY_ONLY.matches(line.trim()) && line.any { it.isLetter() } && canTakeQuantity(prev)) {
                out[out.lastIndex] = "$prev ${line.trim()}"
            } else {
                out += line
            }
        }
        return out
    }

    private fun canTakeQuantity(prev: String): Boolean {
        val n = normalize(prev).trimEnd(':', '?', '.', ' ')
        if (n.length > 60 || n.none { it.isLetter() }) return false
        if (prev.trimEnd().endsWith(':') || prev.trimEnd().endsWith('.')) return false
        if (INGREDIENT_HEADERS.any { n.startsWith(it) } || STEP_HEADERS.any { n.startsWith(it) }) return false
        if (QTY_ONLY.matches(prev.trim()) || parseIngredientLine(prev) != null) return false
        if (KCAL_REGEX.containsMatchIn(n) || SERVINGS_REGEXES.any { it.containsMatchIn(n) }) return false
        return true
    }

    // ---------- Klasyfikacja linii ----------

    private fun classify(raw: String): Line {
        val norm = normalize(raw).trimEnd(':', ' ', '-', '–', '?', '.', '!')
        if (norm.length <= 40 && INGREDIENT_HEADERS.any { norm == it || norm.startsWith("$it ") || norm.startsWith("$it:") || norm.startsWith("$it (") }) {
            return Line(raw, norm, Kind.INGREDIENTS)
        }
        if (norm.length <= 40 && STEP_HEADERS.any { norm == it || norm.startsWith("$it ") || norm.startsWith("$it:") }) {
            return Line(raw, norm, Kind.STEPS)
        }
        if (norm.length <= 45 && (DAY_REGEX.containsMatchIn(norm) || WEEKDAYS.any { norm == it || norm.startsWith("$it ") || norm.startsWith("$it,") })) {
            return Line(raw, norm, Kind.DAY)
        }
        if (norm.length <= 60 && SECTION_WORDS.any { norm.startsWith(it) }) {
            return Line(raw, norm, Kind.SECTION)
        }
        MEAL_REGEX.find(norm)?.let { m ->
            val rest = m.groupValues[3].trim()
            val restOk = rest.isEmpty() || rest.first() in ":-–—|(,0123456789" || norm.length <= 30
            if (restOk && norm.length <= 90) {
                val prefix = m.groupValues[1].trim()
                val meal = when (m.groupValues[2]) {
                    "sniadanie" -> if (prefix.startsWith("ii") || prefix.startsWith("2") || prefix.startsWith("drugie")) {
                        MealType.SECOND_BREAKFAST
                    } else {
                        MealType.BREAKFAST
                    }
                    "obiad", "lunch" -> MealType.LUNCH
                    "podwieczorek", "przekaska" -> MealType.AFTERNOON_SNACK
                    else -> MealType.DINNER
                }
                // Tytuł po nazwie pory, np. „Śniadanie: Owsianka z jabłkiem (420 kcal)”.
                val title = raw.substringAfter(':', "").ifBlank {
                    raw.split(Regex("\\s[-–—|]\\s"), limit = 2).getOrNull(1).orEmpty()
                }.replace(Regex("\\(.*?\\)"), "").trim()
                return Line(raw, norm, Kind.MEAL, meal, title)
            }
        }
        return Line(raw, norm, Kind.OTHER)
    }

    private fun isPageNoise(line: String): Boolean {
        val n = line.trim()
        return n.matches(Regex("^(str(ona)?\\.?\\s*)?\\d{1,3}(\\s*/\\s*\\d{1,3})?$", RegexOption.IGNORE_CASE))
    }

    private fun dayIndexAt(lines: List<Line>, upTo: Int): Int? {
        var count = 0
        var explicit: Int? = null
        for (i in 0..minOf(upTo, lines.size - 1)) {
            if (lines[i].kind == Kind.DAY) {
                count++
                explicit = DAY_REGEX.find(lines[i].norm)?.groupValues?.getOrNull(2)?.toIntOrNull()
            }
        }
        return when {
            count == 0 -> null
            explicit != null -> explicit
            else -> count
        }
    }

    /** Szuka tytułu przepisu powyżej nagłówka „Składniki”. */
    private fun findTitleIndex(lines: List<Line>, header: Int, lowerBound: Int): Int {
        var i = header - 1
        var best = header
        var checked = 0
        while (i >= lowerBound && checked < 6) {
            val line = lines[i]
            when {
                line.kind == Kind.MEAL -> return i
                line.kind == Kind.DAY || line.kind == Kind.STEPS || line.kind == Kind.INGREDIENTS ||
                    line.kind == Kind.SECTION -> return best
                isNutritionLine(line.norm) || isServingsLine(line.norm) -> best = i
                isTitleLike(line.raw) -> return i
                else -> return best
            }
            i--
            checked++
        }
        return best
    }

    private fun isTitleLike(raw: String): Boolean {
        val t = raw.trim()
        if (t.length < 3 || t.length > 70) return false
        if (t.endsWith('.') || t.endsWith(',') || t.endsWith(':') || t.endsWith('?')) return false
        if (STEP_NUMBER.containsMatchIn(t)) return false
        if (parseIngredientLine(t) != null) return false
        return t.first().isLetter()
    }

    // ---------- Budowanie przepisu ----------

    private fun buildRecipe(region: List<Line>, day: Int?, contextMeal: MealType?, @Suppress("UNUSED_PARAMETER") isFirst: Boolean): ImportedRecipe? {
        var title = ""
        var meal = contextMeal
        var section = Kind.OTHER
        val hasHeaders = region.any { it.kind == Kind.INGREDIENTS || it.kind == Kind.STEPS }
        val ingredientLines = mutableListOf<String>()
        val steps = mutableListOf<String>()
        var calories: Int? = null
        var protein: Double? = null
        var carbs: Double? = null
        var fat: Double? = null
        var servings = 1

        for (line in region) {
            when (line.kind) {
                Kind.INGREDIENTS -> {
                    section = Kind.INGREDIENTS
                    SERVINGS_REGEXES.firstNotNullOfOrNull { it.find(line.norm) }?.let { servings = it.groupValues[1].toInt() }
                    continue
                }
                Kind.STEPS -> {
                    section = Kind.STEPS
                    continue
                }
                Kind.DAY, Kind.SECTION -> continue
                Kind.MEAL -> {
                    meal = line.meal
                    if (title.isEmpty() && line.mealRest.isNotBlank()) title = line.mealRest
                    // Wartości w nagłówku posiłku, np. „Śniadanie: Owsianka (420 kcal)”.
                    if (isNutritionLine(line.norm)) {
                        val n = extractNutrition(line.raw, line.norm)
                        if (calories == null) calories = n.kcal
                        if (protein == null) protein = n.protein
                        if (carbs == null) carbs = n.carbs
                        if (fat == null) fat = n.fat
                    }
                    continue
                }
                Kind.OTHER -> Unit
            }

            // Wartości odżywcze i porcje mogą być w dowolnym miejscu.
            if (isNutritionLine(line.norm)) {
                val n = extractNutrition(line.raw, line.norm)
                if (calories == null) calories = n.kcal
                if (protein == null) protein = n.protein
                if (carbs == null) carbs = n.carbs
                if (fat == null) fat = n.fat
                continue
            }
            if (isServingsLine(line.norm)) {
                SERVINGS_REGEXES.firstNotNullOfOrNull { it.find(line.norm) }?.let { servings = it.groupValues[1].toInt() }
                continue
            }

            when {
                section == Kind.INGREDIENTS -> {
                    val longSentence = line.raw.length > 50 && line.raw.trimEnd().endsWith('.') && parseIngredientLine(line.raw) == null
                    if (longSentence && region.none { it.kind == Kind.STEPS }) {
                        appendStep(steps, line.raw)
                    } else {
                        appendIngredient(ingredientLines, line.raw)
                    }
                }
                section == Kind.STEPS -> appendStep(steps, line.raw)
                title.isEmpty() && isTitleLike(line.raw) -> title = line.raw.trim()
                !hasHeaders && parseIngredientLine(line.raw) != null -> ingredientLines += line.raw
                !hasHeaders && (line.raw.startsWith("-") || line.raw.startsWith("•")) -> ingredientLines += line.raw
                !hasHeaders -> appendStep(steps, line.raw)
                title.isEmpty() -> title = line.raw.trim()
            }
        }

        if (title.isBlank() && ingredientLines.isEmpty() && steps.isEmpty()) return null
        if (title.isBlank()) title = meal?.label ?: "Przepis bez nazwy"

        val matched = mutableListOf<RecipeIngredient>()
        val other = mutableListOf<String>()
        ingredientLines.forEach { raw ->
            val parsed = parseIngredientLine(raw)
            val product = matchProduct(parsed?.name ?: cleanBullet(raw))
            val grams = if (parsed != null && product != null) toGrams(parsed.amount, parsed.unit, product) else null
            if (product != null && grams != null && grams > 0) {
                matched += RecipeIngredient.from(product, Math.round(grams * 10.0) / 10.0)
            } else {
                other += cleanBullet(raw)
            }
        }

        return ImportedRecipe(
            title = title.trim().trimEnd(':').take(80),
            category = guessCategory(meal, title),
            mealType = meal,
            dayIndex = day,
            ingredients = matched,
            otherIngredients = other,
            steps = steps.map { it.trim() }.filter { it.isNotEmpty() },
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            servings = servings.coerceIn(1, 20)
        )
    }

    /** Łączy linie kroku rozbite przez OCR (kontynuacja zaczyna się małą literą). */
    private fun appendStep(steps: MutableList<String>, raw: String) {
        val text = raw.trim().replace(STEP_NUMBER, "").trim()
        if (text.isEmpty()) return
        val last = steps.lastOrNull()
        if (last != null && !last.trimEnd().endsWith('.') && !last.trimEnd().endsWith('!') &&
            !last.trimEnd().endsWith(':') && text.first().isLowerCase() && !STEP_NUMBER.containsMatchIn(raw)
        ) {
            steps[steps.lastIndex] = "$last $text"
        } else {
            steps += text
        }
    }

    private fun appendIngredient(lines: MutableList<String>, raw: String) {
        val text = raw.trim()
        val last = lines.lastOrNull()
        if (last != null && text.first().isLowerCase() && parseIngredientLine(text) == null &&
            !text.startsWith("-") && !text.startsWith("•")
        ) {
            lines[lines.lastIndex] = "$last $text"
        } else {
            lines += text
        }
    }

    // ---------- Składniki ----------

    data class ParsedIngredient(val amount: Double, val unit: String?, val name: String)

    fun parseIngredientLine(raw: String): ParsedIngredient? {
        val line = cleanBullet(raw)
        // „Banan 1,5 x sztuka (180 g)” – gramatura w nawiasie jest najdokładniejsza.
        PAREN_GRAMS.find(line)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            val before = line.substring(0, m.range.first).replace(TRAILING_HOUSEHOLD, "").trim().trimEnd('-', '–', ':', ',', ' ')
            val after = line.substring(m.range.last + 1).trim().trimStart('-', '–', ':', ',', ' ')
            val beforeName = before.replace(LEADING_HOUSEHOLD, "").trim()
            val name = beforeName.ifBlank { after }
            if (name.isNotBlank() && name.any { it.isLetter() }) return ParsedIngredient(amount, m.groupValues[2], name)
        }
        LEADING_QTY.find(line)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            val name = m.groupValues[3].trim().trimStart('-', '–', ' ')
            if (name.isNotBlank()) return ParsedIngredient(amount, m.groupValues[2].ifBlank { null }, name)
        }
        TRAILING_QTY.find(line)?.let { m ->
            val amount = parseAmount(m.groupValues[2]) ?: return@let
            val name = m.groupValues[1].trim().trimEnd('-', '–', ':', ',', ' ')
            if (name.isNotBlank()) return ParsedIngredient(amount, m.groupValues[3].ifBlank { null }, name)
        }
        return null
    }

    private fun parseAmount(text: String): Double? = when (text) {
        "½" -> 0.5
        "¼" -> 0.25
        "¾" -> 0.75
        else -> if (text.contains('/')) {
            val (a, b) = text.split('/').map { it.trim().toDoubleOrNull() }
            if (a != null && b != null && b != 0.0) a / b else null
        } else {
            text.replace(',', '.').toDoubleOrNull()
        }
    }

    private fun toGrams(amount: Double, unitRaw: String?, product: FoodProduct): Double? {
        val u = normalize(unitRaw ?: "").trimEnd('.')
        fun unitOf(vararg names: String): Double? =
            if (product.unitName != null && names.any { product.unitName.startsWith(it) }) product.unitGrams?.times(amount) else null
        return when {
            u.isEmpty() -> when {
                amount > 15 -> amount // sama liczba > 15 to zwykle gramy
                product.unitGrams != null -> amount * product.unitGrams
                else -> null
            }
            u == "g" || u.startsWith("gram") -> amount
            u == "dag" -> amount * 10
            u == "kg" -> amount * 1000
            u == "ml" -> amount
            u == "l" || u.startsWith("litr") -> amount * 1000
            u.startsWith("lyzecz") -> unitOf("łyżeczka") ?: amount * 5
            u.startsWith("lyz") -> unitOf("łyżka") ?: amount * 15
            u.startsWith("szklan") -> unitOf("szklanka") ?: amount * 250
            u.startsWith("plast") -> unitOf("plaster") ?: amount * 15
            u.startsWith("kromk") -> unitOf("kromka") ?: amount * 35
            u.startsWith("gars") -> unitOf("garść") ?: amount * 30
            u.startsWith("zab") -> unitOf("ząbek") ?: amount * 5
            u.startsWith("pecz") -> amount * 30
            u.startsWith("szczypt") -> amount * 1
            u.startsWith("kostk") -> unitOf("kostka")
            u.startsWith("puszk") -> unitOf("puszka")
            u.startsWith("szt") || u.startsWith("sztuk") || u.startsWith("opak") || u == "op" ->
                product.unitGrams?.times(amount)
            else -> null
        }
    }

    /** Dopasowanie nazwy składnika do produktu z bazy (odporne na polską odmianę). */
    fun matchProduct(name: String): FoodProduct? {
        val lineTokens = tokens(name)
        if (lineTokens.isEmpty()) return null
        var best: FoodProduct? = null
        var bestScore = 0
        var bestSize = Int.MAX_VALUE
        for ((product, productTokens) in catalog) {
            if (lineTokens.none { stemEquals(productTokens.first(), it) }) continue
            val matched = productTokens.count { p -> lineTokens.any { stemEquals(p, it) } }
            val score = matched * 10 - (productTokens.size - matched) * 3
            if (score > bestScore || (score == bestScore && productTokens.size < bestSize)) {
                best = product
                bestScore = score
                bestSize = productTokens.size
            }
        }
        return best?.takeIf { bestScore >= 7 }
    }

    private fun stemEquals(a: String, b: String): Boolean {
        if (a == b) return true
        if (a.length == b.length && a.length >= 5 && a.zip(b).count { it.first != it.second } == 1) return true
        val common = a.zip(b).takeWhile { it.first == it.second }.count()
        return common >= maxOf(3, maxOf(a.length, b.length) - 3)
    }

    // ---------- Wartości odżywcze ----------

    private fun isNutritionLine(norm: String): Boolean =
        KCAL_REGEX.containsMatchIn(norm) || (MACRO_WORDS.containsMatchIn(norm) && Regex("\\d").containsMatchIn(norm) && norm.length < 120)

    private fun isServingsLine(norm: String): Boolean =
        norm.length < 40 && SERVINGS_REGEXES.any { it.containsMatchIn(norm) } && !KCAL_REGEX.containsMatchIn(norm)

    private data class Nutrition(val kcal: Int?, val protein: Double?, val carbs: Double?, val fat: Double?)

    private fun extractNutrition(raw: String, norm: String): Nutrition {
        val kcal = KCAL_REGEX.find(norm)?.groupValues?.get(1)?.toIntOrNull()
        fun word(regex: Regex, letter: Char): Double? =
            regex.find(norm)?.groupValues?.get(2)?.replace(',', '.')?.toDoubleOrNull()
                ?: Regex("(?<![A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśźż])$letter\\s*[:=]?\\s*(\\d+(?:[.,]\\d+)?)\\s*g").find(raw)
                    ?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        return Nutrition(kcal, word(PROTEIN_REGEX, 'B'), word(CARBS_REGEX, 'W'), word(FAT_REGEX, 'T'))
    }

    private fun guessCategory(meal: MealType?, title: String): RecipeCategory {
        when (meal) {
            MealType.BREAKFAST, MealType.SECOND_BREAKFAST -> return RecipeCategory.BREAKFAST
            MealType.LUNCH -> return RecipeCategory.LUNCH
            MealType.AFTERNOON_SNACK -> return RecipeCategory.SNACK
            MealType.DINNER -> return RecipeCategory.DINNER
            null -> Unit
        }
        val t = normalize(title)
        return when {
            listOf("owsian", "jajecz", "omlet", "placusz", "naleśn", "nalesn", "granol", "twarozek", "musli", "kanapk").any { t.contains(it) } ->
                RecipeCategory.BREAKFAST
            listOf("koktajl", "smoothie", "deser", "batonik", "ciast", "muffin", "pudding", "przekask").any { t.contains(it) } ->
                RecipeCategory.SNACK
            listOf("salatk", "tost", "wrap", "zapiekank").any { t.contains(it) } -> RecipeCategory.DINNER
            else -> RecipeCategory.LUNCH
        }
    }

    // ---------- Pomocnicze ----------

    private fun cleanBullet(raw: String) = raw.trim().trimStart('-', '•', '*', '·', '▪', '–', '—', ' ').trim()

    private fun tokens(text: String): List<String> =
        normalize(text).split(Regex("[^a-z0-9]+")).filter { it.length >= 3 && it !in STOP_WORDS && !it.all(Char::isDigit) }

    companion object {
        fun normalize(text: String): String =
            Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .replace('ł', 'l')
                .replace(Regex("\\s+"), " ")
                .trim()

        private val INGREDIENT_HEADERS = listOf("skladniki", "lista skladnikow", "potrzebne skladniki", "skladniki na")
        private val STEP_HEADERS = listOf(
            "przygotowanie", "sposob przygotowania", "sposob wykonania", "wykonanie", "instrukcja",
            "instrukcje", "przepis", "jak przygotowac", "kroki", "przyrzadzenie", "jak to zrobic", "jak zrobic", "sposob przyrzadzenia", "jak przyrzadzic"
        )
        private val SECTION_WORDS = listOf("jadlospis", "plan zywieniowy", "plan posilkow", "menu na", "spis tresci", "lista zakupow")
        private val WEEKDAYS = listOf("poniedzialek", "wtorek", "sroda", "czwartek", "piatek", "sobota", "niedziela")
        private val DAY_REGEX = Regex("^(dzien|day)\\s*(\\d{1,2})\\b")
        private val MEAL_REGEX = Regex("^((?:ii|i|2|1|drugie|pierwsze)\\.?\\s+)?(sniadanie|obiad|lunch|podwieczorek|przekaska|kolacja)\\b(.*)$")
        private val STEP_NUMBER = Regex("^\\s*((krok\\s*)?\\d{1,2}[.)]|krok\\s*\\d{1,2}:?)\\s+", RegexOption.IGNORE_CASE)
        private val UNIT = "kg|dag|g|gram\\w*|ml|l|litr\\w*|szt\\.?|sztuk\\w*|łyż\\w*|łyz\\w*|lyz\\w*|szklan\\w*|plastr\\w*|plaster|kromk\\w*|garś\\w*|gars\\w*|ząb\\w*|zab\\w*|opak\\w*|op\\.?|puszk\\w*|pęcz\\w*|pecz\\w*|kostk\\w*|szczypt\\w*"
        private val AMOUNT = "\\d+(?:[.,]\\d+)?|½|¼|¾|\\d+\\s*/\\s*\\d+"
        private val LEADING_QTY = Regex("^($AMOUNT)\\s*(?:x\\s*)?($UNIT)?\\.?\\s+(.+)$", RegexOption.IGNORE_CASE)
        private val TRAILING_QTY = Regex("^(.+?)\\s*[-–—:,]?\\s*\\(?\\s*($AMOUNT)\\s*(?:x\\s*)?($UNIT)\\.?\\s*\\)?$", RegexOption.IGNORE_CASE)
        private val PAREN_GRAMS = Regex("\\(\\s*(?:ok\\.?\\s*|około\\s*|~\\s*)?($AMOUNT)\\s*(g|ml|kg|dag)\\s*\\)", RegexOption.IGNORE_CASE)
        private val TRAILING_HOUSEHOLD = Regex("\\s*($AMOUNT)\\s*(x\\s*)?[\\p{L}.]*\\s*$")
        private val LEADING_HOUSEHOLD = Regex("^($AMOUNT)\\s*(x\\s*)?[\\p{L}.]*\\s+(?=\\p{L})")
        /** Linia zawierająca wyłącznie ilość, np. „25 g”, „1x szklanka (250 g)”, „1,5 x sztuka (180 g)”. */
        private val QTY_ONLY = Regex(
            // „25 g”, „2 łyżki” – znana jednostka…
            "^($AMOUNT)\\s*($UNIT|sztuk\\w*|szczypt\\w*|łyżecz\\w*|łyżk\\w*|szklank\\w*|kawał\\w*|plastr\\w*|porcj\\w*|opakowan\\w*|ząb\\w*|garś\\w*|kubek|kubk\\w*|filiżank\\w*)\\.?$" +
                // …albo „1,5 x łyżeczka (15 g)” / „2x tyżka” – po „x” lub przed gramaturą w nawiasie dowolne słowo (OCR przekręca jednostki).
                "|^($AMOUNT)\\s*(?:x\\s*[\\p{L}.]+|x?\\s*[\\p{L}.]*\\s*\\(\\s*(?:ok\\.?\\s*)?($AMOUNT)\\s*(g|ml|kg|dag)\\s*\\))$" +
                "|^($AMOUNT)\\s*x\\s*[\\p{L}.]+\\s*\\(\\s*(?:ok\\.?\\s*)?($AMOUNT)\\s*(g|ml|kg|dag)\\s*\\)$",
            RegexOption.IGNORE_CASE
        )
        private val TIME_OF_DAY = Regex("^\\d{1,2}:\\d{2}$")
        private val STATUS_BAR = Regex("\\b(volte|lte|vo lte)\\b|^vo\\b|^\\d{1,2}:\\d{2}\\s+\\S")
        private val UI_NOISE = listOf(
            Regex("^ocen przepis"), Regex("\\d\\s*ocen\\w*$"), Regex("^\\d[.,]\\d\\s*[·•.-]?\\s*\\d+\\s*ocen"),
            Regex("^posilek \\d+$"), Regex("^na (jedna )?porcj\\w* przypada"), Regex("^wartosci odzywcze( na porcje| w porcji)?$"),
            Regex("^(dodaj do (dziennika|listy|ulubionych|planu)|udostepnij|zapisz przepis|komentarze?|zobacz (wiecej|tez))$"),
            Regex("^(zamien|wymien) (produkt|skladnik|posilek)")
        )
        private val SERVINGS_QUESTION = Regex("^[il1|]le porcji")
        private val TIME_ONLY = Regex("^\\d+\\s*(min|minut\\w*|h|godz\\w*)$")
        private val TIME_WITH_LABEL = Regex("^\\d+\\s*(min|minut\\w*|h|godz\\w*)\\.?\\s+(przygotowanie|laczny czas|gotowanie|pieczenie|czas \\w+)$")
        private val TIME_LABELS = setOf("przygotowanie", "gotowanie", "pieczenie", "czas przygotowania", "czas gotowania")
        private val KCAL_REGEX = Regex("(\\d{2,4})\\s*kcal")
        private val PROTEIN_REGEX = Regex("(bialko|bialka)\\s*[:=-]?\\s*(\\d+(?:[.,]\\d+)?)\\s*g")
        private val CARBS_REGEX = Regex("(weglowodany|weglowodanow)\\s*[:=-]?\\s*(\\d+(?:[.,]\\d+)?)\\s*g")
        private val FAT_REGEX = Regex("(tluszcze|tluszcz|tluszczu)\\s*[:=-]?\\s*(\\d+(?:[.,]\\d+)?)\\s*g")
        private val MACRO_WORDS = Regex("\\b(bialko|bialka|weglowodan\\w*|tluszcz|tluszcze|tluszczu|kcal)\\b")
        private val SERVINGS_REGEXES = listOf(
            Regex("(\\d{1,2})\\s*porcj"),
            Regex("porcj\\w*\\s*[:=]?\\s*(\\d{1,2})"),
            Regex("na\\s*(\\d{1,2})\\s*os")
        )
        private val STOP_WORDS = setOf(
            "oraz", "lub", "albo", "okolo", "dla", "bez", "swieze", "swiezy", "swiezej", "duze", "duzy", "duza",
            "maly", "mala", "male", "srednie", "sredni", "srednia", "drobno", "posiekane", "posiekany", "pokrojony",
            "pokrojone", "smaku", "szczypta", "opcjonalnie", "the", "and"
        )
    }
}
