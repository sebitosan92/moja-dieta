package com.example.mojadieta.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Wypełnia bazę przykładowymi danymi przy pierwszym utworzeniu (pierwsze uruchomienie aplikacji).
 * Wstawianie odbywa się synchronicznie przez ContentValues, więc nie ma problemu z polskimi
 * znakami ani z cudzysłowami w treści.
 */
class PrepopulateCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        sampleRecipes.forEach { recipe ->
            db.insert("recipes", SQLiteDatabase.CONFLICT_ABORT, recipe)
        }
        val now = System.currentTimeMillis()
        db.insert(
            "notes",
            SQLiteDatabase.CONFLICT_ABORT,
            ContentValues().apply {
                put("title", "Lista zakupów i cele na tydzień")
                put(
                    "content",
                    """
                    Zakupy:
                    - płatki owsiane górskie
                    - borówki (świeże lub mrożone)
                    - pierś z kurczaka 500 g
                    - ryż basmati
                    - jajka (10 szt.)
                    - awokado x2
                    - chleb żytni razowy

                    Cele:
                    • pić min. 2,5 l wody dziennie
                    • 5 posiłków co 3–4 godziny
                    • warzywa do każdego głównego posiłku

                    Pomiary (start): waga 72,4 kg, talia 81 cm. Samopoczucie: dobre, więcej energii po śniadaniu.
                    """.trimIndent()
                )
                put("createdAt", now)
                put("updatedAt", now)
            }
        )
    }

    private fun recipe(
        name: String,
        category: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        ingredients: String,
        instructions: String
    ) = ContentValues().apply {
        put("name", name)
        put("category", category)
        put("calories", calories)
        put("protein", protein)
        put("carbs", carbs)
        put("fat", fat)
        put("ingredients", ingredients)
        put("instructions", instructions)
    }

    private val sampleRecipes = listOf(
        recipe(
            name = "Owsianka z borówkami",
            category = "BREAKFAST",
            calories = 410,
            protein = 15.0,
            carbs = 62.0,
            fat = 11.0,
            ingredients = """
                60 g płatków owsianych górskich
                250 ml mleka 2%
                100 g borówek amerykańskich
                1 łyżeczka miodu
                10 g orzechów włoskich
                szczypta cynamonu
            """.trimIndent(),
            instructions = """
                Wsyp płatki owsiane do garnka i zalej je mlekiem.
                Gotuj na małym ogniu przez 5–7 minut, często mieszając, aż owsianka zgęstnieje.
                Dodaj cynamon i miód, wymieszaj.
                Przełóż do miski, posyp borówkami i posiekanymi orzechami włoskimi.
            """.trimIndent()
        ),
        recipe(
            name = "Kurczak z ryżem i warzywami",
            category = "LUNCH",
            calories = 620,
            protein = 45.0,
            carbs = 70.0,
            fat = 15.0,
            ingredients = """
                150 g piersi z kurczaka
                70 g ryżu basmati (suchego)
                1 papryka czerwona
                1 mała cukinia
                100 g brokułów
                1 łyżka oliwy z oliwek
                1 ząbek czosnku
                sól, pieprz, słodka papryka, zioła prowansalskie
            """.trimIndent(),
            instructions = """
                Ugotuj ryż zgodnie z instrukcją na opakowaniu.
                Pierś z kurczaka pokrój w kostkę i dopraw solą, pieprzem i słodką papryką.
                Rozgrzej oliwę na patelni i smaż kurczaka około 6–8 minut, aż będzie złocisty.
                Dodaj pokrojoną paprykę, cukinię, różyczki brokułów i przeciśnięty czosnek.
                Duś pod przykryciem 5–7 minut, dopraw ziołami prowansalskimi.
                Podawaj z ugotowanym ryżem.
            """.trimIndent()
        ),
        recipe(
            name = "Jajecznica na maśle",
            category = "BREAKFAST",
            calories = 380,
            protein = 22.0,
            carbs = 18.0,
            fat = 25.0,
            ingredients = """
                3 jajka
                10 g masła
                1 kromka chleba żytniego
                2 łyżki posiekanego szczypiorku
                5 pomidorków koktajlowych
                sól, pieprz
            """.trimIndent(),
            instructions = """
                Rozpuść masło na patelni na małym ogniu.
                Wbij jajka i delikatnie mieszaj szpatułką, aż białka się zetną, a jajecznica będzie kremowa.
                Dopraw solą i pieprzem, posyp szczypiorkiem.
                Podawaj z kromką chleba żytniego i pomidorkami koktajlowymi.
            """.trimIndent()
        ),
        recipe(
            name = "Kanapki z pastą z awokado",
            category = "DINNER",
            calories = 450,
            protein = 14.0,
            carbs = 45.0,
            fat = 24.0,
            ingredients = """
                1 dojrzałe awokado
                2 kromki chleba pełnoziarnistego
                1 łyżeczka soku z cytryny
                1 jajko ugotowane na twardo
                garść rukoli
                płatki chili, sól, pieprz
            """.trimIndent(),
            instructions = """
                Przekrój awokado, usuń pestkę i wydrąż miąższ łyżką.
                Rozgnieć awokado widelcem z sokiem z cytryny, solą i pieprzem.
                Posmaruj pastą kromki chleba.
                Ułóż na wierzchu rukolę i plasterki jajka, posyp płatkami chili.
            """.trimIndent()
        )
    )
}
