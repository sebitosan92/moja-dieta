package com.example.mojadieta

import com.example.mojadieta.data.food.FoodCatalog
import com.example.mojadieta.domain.importer.RecipeTextParser
import com.example.mojadieta.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTextParserTest {

    private val parser = RecipeTextParser(FoodCatalog.products)

    @Test
    fun singleRecipeWithHeaders() {
        val text = """
            Owsianka z bananem i masłem orzechowym
            Porcje: 2
            Składniki:
            - 80 g płatków owsianych
            - 400 ml mleka 2%
            - 1 banan
            - 2 łyżki masła orzechowego
            - szczypta cynamonu
            Przygotowanie:
            1. Płatki zalej mlekiem i gotuj 5 minut,
            często mieszając.
            2. Dodaj pokrojonego banana i masło orzechowe.
        """.trimIndent()
        val result = parser.parse(text)
        assertEquals(1, result.recipes.size)
        val r = result.recipes.first()
        assertEquals("Owsianka z bananem i masłem orzechowym", r.title)
        assertEquals(2, r.servings)
        val names = r.ingredients.map { it.name }
        assertTrue(names.toString(), "Płatki owsiane" in names)
        assertTrue(names.toString(), "Mleko 2%" in names)
        assertTrue(names.toString(), "Banan" in names)
        assertTrue(names.toString(), "Masło orzechowe" in names)
        assertEquals(80.0, r.ingredients.first { it.name == "Płatki owsiane" }.grams, 0.01)
        assertEquals(120.0, r.ingredients.first { it.name == "Banan" }.grams, 0.01)
        assertEquals(30.0, r.ingredients.first { it.name == "Masło orzechowe" }.grams, 0.01)
        assertEquals(listOf("szczypta cynamonu"), r.otherIngredients)
        assertEquals(2, r.steps.size)
        assertEquals("Płatki zalej mlekiem i gotuj 5 minut, często mieszając.", r.steps[0])
        assertFalse(r.hasExplicitNutrition)
    }

    @Test
    fun ebookWithManyRecipesAndNutrition() {
        val text = """
            Kurczak curry z ryżem
            520 kcal | B 38 g | W 60 g | T 12 g
            Składniki
            150 g piersi z kurczaka
            70 g ryżu basmati
            1 cebula
            Przygotowanie
            Pokrój kurczaka i podsmaż z cebulą.
            Podawaj z ryżem.
            Sałatka grecka
            Składniki
            2 pomidory
            1 ogórek
            50 g sera feta
            Sposób przygotowania
            Pokrój warzywa i wymieszaj z fetą.
        """.trimIndent()
        val result = parser.parse(text)
        assertEquals(2, result.recipes.size)
        val curry = result.recipes[0]
        assertEquals("Kurczak curry z ryżem", curry.title)
        assertEquals(520, curry.calories)
        assertEquals(38.0, curry.protein!!, 0.01)
        assertEquals(60.0, curry.carbs!!, 0.01)
        assertEquals(12.0, curry.fat!!, 0.01)
        assertEquals(2, curry.steps.size)
        assertTrue(curry.ingredients.any { it.name == "Pierś z kurczaka" && it.grams == 150.0 })
        assertTrue(curry.ingredients.any { it.name == "Cebula" && it.grams == 100.0 })
        val salad = result.recipes[1]
        assertEquals("Sałatka grecka", salad.title)
        assertTrue(salad.ingredients.any { it.name == "Pomidor" && it.grams == 300.0 })
        assertTrue(salad.ingredients.any { it.name == "Ser feta" && it.grams == 50.0 })
        assertEquals(1, salad.steps.size)
    }

    @Test
    fun mealPlanWithDays() {
        val text = """
            Jadłospis 1500 kcal
            Dzień 1
            Śniadanie: Jajecznica z pomidorami (380 kcal)
            3 jajka
            1 pomidor
            1 kromka chleba żytniego
            Obiad: Łosoś z kaszą
            120 g łososia
            60 g kaszy gryczanej
            Kolacja: Kanapki z twarożkiem
            2 kromki chleba pełnoziarnistego
            100 g twarogu półtłustego
            Dzień 2
            II śniadanie: Jogurt z owocami
            150 g jogurtu naturalnego
            garść borówek
        """.trimIndent()
        val result = parser.parse(text)
        assertTrue(result.isMealPlan)
        assertEquals(4, result.recipes.size)
        val breakfast = result.recipes[0]
        assertEquals("Jajecznica z pomidorami", breakfast.title)
        assertEquals(MealType.BREAKFAST, breakfast.mealType)
        assertEquals(1, breakfast.dayIndex)
        assertEquals(380, breakfast.calories)
        assertTrue(breakfast.ingredients.any { it.name == "Jajko kurze" && it.grams == 150.0 })
        assertEquals(MealType.LUNCH, result.recipes[1].mealType)
        assertEquals(MealType.DINNER, result.recipes[2].mealType)
        val day2 = result.recipes[3]
        assertEquals(2, day2.dayIndex)
        assertEquals(MealType.SECOND_BREAKFAST, day2.mealType)
        assertEquals("Jogurt z owocami", day2.title)
    }

    @Test
    fun screenshotWithoutHeaders() {
        val text = """
            Szybkie pancakes bananowe
            1 banan
            2 jajka
            30 g mąki owsianej
            Rozgnieć banana widelcem, dodaj jajka i mąkę.
            Smaż na rozgrzanej patelni z obu stron.
        """.trimIndent()
        val r = parser.parse(text).recipes.single()
        assertEquals("Szybkie pancakes bananowe", r.title)
        assertEquals(3, r.ingredients.size)
        assertEquals(2, r.steps.size)
    }

    @Test
    fun productMatchingHandlesPolishInflection() {
        assertEquals("Płatki owsiane", parser.matchProduct("płatków owsianych")?.name)
        assertEquals("Mąka pszenna", parser.matchProduct("mąki pszennej")?.name)
        assertEquals("Jajko kurze", parser.matchProduct("jajka")?.name)
        assertEquals("Pierś z kurczaka", parser.matchProduct("piersi z kurczaka")?.name)
        assertNull(parser.matchProduct("szczypta soli i pieprzu do smaku xyz"))
        assertNotNull(parser.parseIngredientLine("Płatki owsiane – 60 g"))
        assertEquals(60.0, parser.parseIngredientLine("Płatki owsiane – 60 g")!!.amount, 0.01)
    }

    @Test
    fun mixedEbookAndMealPlan() {
        val text = """
            Makaron z tuńczykiem
            480 kcal | B 35 g | W 55 g | T 12 g
            Składniki:
            - 80 g makaronu pełnoziarnistego
            - 1 puszka tuńczyka w wodzie
            Przygotowanie:
            Ugotuj makaron al dente. Na patelni podgrzej
            passatę z czosnkiem, dodaj tuńczyka i makaron.
            Jadłospis – tydzień 1
            Dzień 1
            Śniadanie: Jogurt z owocami
            150 g jogurtu naturalnego
            1 banan
            Obiad: Kurczak z kaszą
            150 g piersi z kurczaka
            Dzień 2
            Kolacja: Sałatka z tuńczykiem
            1 ogórek
        """.trimIndent()
        val result = parser.parse(text)
        assertEquals(result.recipes.map { it.title }.toString(), 4, result.recipes.size)
        val pasta = result.recipes[0]
        assertEquals("Makaron z tuńczykiem", pasta.title)
        assertEquals(1, pasta.steps.size)
        assertNull(pasta.dayIndex)
        assertNull(pasta.mealType)
        assertEquals("Jogurt z owocami", result.recipes[1].title)
        assertEquals(1, result.recipes[1].dayIndex)
        assertEquals(MealType.LUNCH, result.recipes[2].mealType)
        assertEquals(2, result.recipes[3].dayIndex)
        assertEquals(MealType.DINNER, result.recipes[3].mealType)
    }

    private fun assertFalse(value: Boolean) = assertTrue(!value)
}
