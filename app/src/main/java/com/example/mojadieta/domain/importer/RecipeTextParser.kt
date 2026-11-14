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
        val lines = text.lines()
            .map { it.replace(' ', ' ').trim() }
            .filter { it.isNotEmpty() && !isPageNoise(it) }
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

    // ---------- Klasyfikacja linii ----------

    private fun classify(raw: String): Line {
        val norm = normalize(raw).trimEnd(':', ' ', '-', '–')
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
        if (t.endsWith('.') || t.endsWith(',')) return false
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
            "instrukcje", "przepis", "jak przygotowac", "kroki", "przyrzadzenie"
        )
        private val SECTION_WORDS = listOf("jadlospis", "plan zywieniowy", "plan posilkow", "menu na", "spis tresci", "lista zakupow")
        private val WEEKDAYS = listOf("poniedzialek", "wtorek", "sroda", "czwartek", "piatek", "sobota", "niedziela")
        private val DAY_REGEX = Regex("^(dzien|day)\\s*(\\d{1,2})\\b")
        private val MEAL_REGEX = Regex("^((?:ii|i|2|1|drugie|pierwsze)\\.?\\s+)?(sniadanie|obiad|lunch|podwieczorek|przekaska|kolacja)\\b(.*)$")
        private val STEP_NUMBER = Regex("^\\s*((krok\\s*)?\\d{1,2}[.)]|krok\\s*\\d{1,2}:?)\\s+", RegexOption.IGNORE_CASE)
        private val UNIT = "kg|dag|g|gram\\w*|ml|l|litr\\w*|szt\\.?|sztuk\\w*|łyż\\w*|łyz\\w*|lyz\\w*|szklan\\w*|plastr\\w*|plaster|kromk\\w*|garś\\w*|gars\\w*|ząb\\w*|zab\\w*|opak\\w*|op\\.?|puszk\\w*|pęcz\\w*|pecz\\w*|kostk\\w*"
        private val AMOUNT = "\\d+(?:[.,]\\d+)?|½|¼|¾|\\d+\\s*/\\s*\\d+"
        private val LEADING_QTY = Regex("^($AMOUNT)\\s*($UNIT)?\\.?\\s+(.+)$", RegexOption.IGNORE_CASE)
        private val TRAILING_QTY = Regex("^(.+?)\\s*[-–—:,]?\\s*\\(?\\s*($AMOUNT)\\s*($UNIT)\\.?\\s*\\)?$", RegexOption.IGNORE_CASE)
        private val KCAL_REGEX = Regex("(\\d{2,4})\\s*kcal")
        private val PROTEIN_REGEX = Regex("(bialko|bialka)\\s*[:=-]?\\s*(\\d+(?:[.,]\\d+)?)\\s*g")
        private val CARBS_REGEX = Regex("(weglowodany|weglowodanow)\\s*[:=-]?\\s*(\\d+(?:[.,]\\d+)?)\\s*g")
        private val FAT_REGEX = Regex("(tluszcze|tluszcz|tluszczu)\\s*[:=-]?\\s*(\\d+(?:[.,]\\d+)?)\\s*g")
        private val MACRO_WORDS = Regex("bialk|weglowodan|tluszcz|\\bkcal\\b")
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
