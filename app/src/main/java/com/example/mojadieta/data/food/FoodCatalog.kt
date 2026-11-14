package com.example.mojadieta.data.food

import com.example.mojadieta.domain.model.FoodProduct
import java.text.Normalizer

/**
 * Wbudowana baza popularnych produktów. Wartości orientacyjne na 100 g
 * (kcal, białko, węglowodany, tłuszcze) + typowa porcja.
 */
object FoodCatalog {

    private fun p(
        name: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        unit: String? = null,
        unitGrams: Double? = null
    ) = FoodProduct(name, kcal, protein, carbs, fat, unit, unitGrams)

    val products: List<FoodProduct> = listOf(
        // Pieczywo i zboża
        p("Chleb żytni razowy", 213.0, 5.9, 41.2, 1.8, "kromka", 35.0),
        p("Chleb pszenny", 260.0, 8.5, 50.0, 3.0, "kromka", 30.0),
        p("Chleb pełnoziarnisty", 245.0, 9.0, 43.0, 3.5, "kromka", 35.0),
        p("Bułka pszenna", 275.0, 9.0, 55.0, 2.5, "szt.", 50.0),
        p("Bułka grahamka", 250.0, 9.0, 47.0, 3.0, "szt.", 60.0),
        p("Tortilla pszenna", 310.0, 8.5, 52.0, 7.5, "szt.", 60.0),
        p("Wafle ryżowe", 387.0, 8.0, 81.0, 2.8, "szt.", 10.0),
        p("Płatki owsiane", 372.0, 13.5, 58.7, 7.0, "łyżka", 10.0),
        p("Płatki kukurydziane", 380.0, 7.0, 84.0, 1.0, "szklanka", 30.0),
        p("Musli bez cukru", 370.0, 10.0, 60.0, 8.0, "łyżka", 12.0),
        p("Granola", 470.0, 10.0, 64.0, 20.0, "łyżka", 12.0),
        p("Ryż biały (suchy)", 345.0, 6.7, 78.9, 0.7, "woreczek", 100.0),
        p("Ryż brązowy (suchy)", 350.0, 7.5, 72.0, 2.7, "woreczek", 100.0),
        p("Ryż basmati (suchy)", 350.0, 8.0, 77.0, 1.0, "woreczek", 100.0),
        p("Ryż biały (ugotowany)", 130.0, 2.7, 28.0, 0.3),
        p("Kasza gryczana (sucha)", 343.0, 13.0, 70.0, 3.4, "woreczek", 100.0),
        p("Kasza jaglana (sucha)", 348.0, 10.5, 71.6, 2.9, "woreczek", 100.0),
        p("Kasza bulgur (sucha)", 342.0, 12.0, 76.0, 1.3, "woreczek", 100.0),
        p("Kasza kuskus (sucha)", 360.0, 12.8, 72.0, 0.6),
        p("Komosa ryżowa (sucha)", 368.0, 14.0, 64.0, 6.0),
        p("Makaron pszenny (suchy)", 360.0, 12.5, 72.0, 1.5),
        p("Makaron pełnoziarnisty (suchy)", 345.0, 13.5, 66.0, 2.5),
        p("Makaron (ugotowany)", 158.0, 5.8, 31.0, 0.9),
        p("Mąka pszenna", 345.0, 10.0, 72.0, 1.2, "łyżka", 10.0),
        p("Ziemniaki", 77.0, 2.0, 17.0, 0.1, "szt.", 100.0),
        p("Bataty", 86.0, 1.6, 20.0, 0.1, "szt.", 200.0),

        // Nabiał i jajka
        p("Jajko kurze", 140.0, 12.5, 0.7, 9.7, "szt.", 50.0),
        p("Białko jaja", 48.0, 10.9, 0.7, 0.2, "szt.", 33.0),
        p("Mleko 2%", 51.0, 3.4, 4.9, 2.0, "szklanka", 250.0),
        p("Mleko 3,2%", 61.0, 3.2, 4.8, 3.2, "szklanka", 250.0),
        p("Napój owsiany", 45.0, 1.0, 6.5, 1.5, "szklanka", 250.0),
        p("Napój sojowy", 40.0, 3.3, 2.5, 1.8, "szklanka", 250.0),
        p("Jogurt naturalny 2%", 61.0, 4.3, 6.2, 2.0, "kubek", 150.0),
        p("Jogurt grecki", 133.0, 4.0, 4.0, 10.0, "kubek", 150.0),
        p("Skyr naturalny", 63.0, 11.0, 4.0, 0.2, "kubek", 150.0),
        p("Kefir", 50.0, 3.4, 4.5, 2.0, "szklanka", 250.0),
        p("Twaróg półtłusty", 133.0, 18.7, 3.7, 4.7, "kostka", 250.0),
        p("Twaróg chudy", 99.0, 19.8, 3.5, 0.5, "kostka", 250.0),
        p("Serek wiejski", 97.0, 11.0, 2.0, 5.0, "opak.", 200.0),
        p("Serek twarogowy kanapkowy", 250.0, 7.0, 3.0, 23.0, "łyżka", 15.0),
        p("Ser żółty gouda", 356.0, 25.0, 0.0, 28.0, "plaster", 15.0),
        p("Mozzarella", 250.0, 18.0, 2.0, 19.0, "kulka", 125.0),
        p("Ser feta", 264.0, 14.0, 4.0, 21.0),
        p("Parmezan", 431.0, 38.0, 4.0, 29.0, "łyżka", 5.0),
        p("Ser camembert", 300.0, 20.0, 0.5, 24.0),
        p("Masło", 735.0, 0.7, 0.7, 82.0, "łyżeczka", 5.0),
        p("Śmietana 18%", 186.0, 2.5, 3.6, 18.0, "łyżka", 15.0),

        // Mięso, wędliny i ryby
        p("Pierś z kurczaka", 99.0, 21.5, 0.0, 1.3, "szt.", 200.0),
        p("Udko z kurczaka bez skóry", 120.0, 19.0, 0.0, 5.0, "szt.", 100.0),
        p("Pierś z indyka", 84.0, 19.2, 0.0, 1.0),
        p("Mięso mielone wołowe", 250.0, 17.0, 0.0, 20.0),
        p("Wołowina chuda", 113.0, 21.0, 0.0, 3.0),
        p("Schab wieprzowy", 172.0, 21.0, 0.0, 9.6),
        p("Polędwica sopocka", 103.0, 18.0, 1.0, 3.0, "plaster", 10.0),
        p("Szynka z indyka", 95.0, 17.0, 1.5, 2.5, "plaster", 10.0),
        p("Boczek", 350.0, 14.0, 0.0, 33.0, "plaster", 15.0),
        p("Kiełbasa", 300.0, 14.0, 1.0, 27.0),
        p("Łosoś świeży", 201.0, 20.0, 0.0, 13.6),
        p("Łosoś wędzony", 162.0, 21.5, 0.0, 8.4),
        p("Dorsz", 77.0, 17.7, 0.0, 0.7),
        p("Pstrąg", 119.0, 20.5, 0.0, 4.0),
        p("Tuńczyk w wodzie (puszka)", 103.0, 24.0, 0.0, 0.8, "puszka", 120.0),
        p("Makrela wędzona", 220.0, 21.0, 0.0, 15.0),
        p("Krewetki", 85.0, 19.0, 1.0, 1.0),

        // Strączki i zamienniki mięsa
        p("Ciecierzyca (puszka)", 125.0, 7.5, 17.0, 2.5, "puszka", 240.0),
        p("Fasola czerwona (puszka)", 100.0, 7.0, 14.0, 0.5, "puszka", 240.0),
        p("Soczewica czerwona (sucha)", 330.0, 25.0, 50.0, 1.5),
        p("Soczewica (ugotowana)", 116.0, 9.0, 20.0, 0.4),
        p("Tofu naturalne", 120.0, 13.0, 2.0, 7.0, "kostka", 180.0),
        p("Groszek zielony", 75.0, 5.5, 13.0, 0.4),
        p("Edamame", 121.0, 12.0, 9.0, 5.0),
        p("Hummus", 166.0, 7.9, 14.0, 9.6, "łyżka", 20.0),

        // Warzywa
        p("Pomidor", 18.0, 0.9, 3.9, 0.2, "szt.", 150.0),
        p("Pomidorki koktajlowe", 18.0, 0.9, 3.9, 0.2, "szt.", 15.0),
        p("Ogórek", 15.0, 0.7, 3.6, 0.1, "szt.", 180.0),
        p("Papryka czerwona", 31.0, 1.0, 6.0, 0.3, "szt.", 180.0),
        p("Cebula", 40.0, 1.1, 9.3, 0.1, "szt.", 100.0),
        p("Czosnek", 149.0, 6.4, 33.0, 0.5, "ząbek", 5.0),
        p("Marchew", 41.0, 0.9, 9.6, 0.2, "szt.", 80.0),
        p("Brokuł", 34.0, 2.8, 6.6, 0.4, "różyczka", 20.0),
        p("Kalafior", 25.0, 1.9, 5.0, 0.3),
        p("Cukinia", 17.0, 1.2, 3.1, 0.3, "szt.", 250.0),
        p("Bakłażan", 25.0, 1.0, 6.0, 0.2, "szt.", 300.0),
        p("Szpinak", 23.0, 2.9, 3.6, 0.4, "garść", 30.0),
        p("Sałata", 15.0, 1.4, 2.9, 0.2, "liść", 10.0),
        p("Rukola", 25.0, 2.6, 3.7, 0.7, "garść", 20.0),
        p("Kapusta biała", 25.0, 1.3, 5.8, 0.1),
        p("Kapusta kiszona", 18.0, 1.0, 3.4, 0.2),
        p("Pieczarki", 22.0, 3.1, 3.3, 0.3, "szt.", 20.0),
        p("Kukurydza (puszka)", 85.0, 2.9, 15.0, 1.2, "łyżka", 20.0),
        p("Burak", 43.0, 1.6, 10.0, 0.2, "szt.", 150.0),
        p("Por", 61.0, 1.5, 14.0, 0.3),
        p("Seler naciowy", 16.0, 0.7, 3.0, 0.2, "łodyga", 40.0),
        p("Fasolka szparagowa", 31.0, 1.8, 7.0, 0.2),
        p("Dynia", 26.0, 1.0, 6.5, 0.1),
        p("Awokado", 160.0, 2.0, 8.5, 14.7, "szt.", 150.0),
        p("Oliwki zielone", 145.0, 1.0, 3.8, 15.0, "szt.", 4.0),
        p("Szczypiorek", 30.0, 3.3, 4.4, 0.7, "łyżka", 5.0),
        p("Natka pietruszki", 36.0, 3.0, 6.3, 0.8, "łyżka", 5.0),
        p("Passata pomidorowa", 30.0, 1.4, 5.5, 0.2, "szklanka", 250.0),
        p("Koncentrat pomidorowy", 82.0, 4.3, 19.0, 0.5, "łyżka", 20.0),

        // Owoce
        p("Jabłko", 52.0, 0.3, 14.0, 0.2, "szt.", 180.0),
        p("Banan", 89.0, 1.1, 23.0, 0.3, "szt.", 120.0),
        p("Pomarańcza", 47.0, 0.9, 12.0, 0.1, "szt.", 200.0),
        p("Mandarynka", 53.0, 0.8, 13.0, 0.3, "szt.", 80.0),
        p("Gruszka", 57.0, 0.4, 15.0, 0.1, "szt.", 180.0),
        p("Truskawki", 32.0, 0.7, 7.7, 0.3, "garść", 80.0),
        p("Borówki amerykańskie", 57.0, 0.7, 14.5, 0.3, "garść", 50.0),
        p("Maliny", 52.0, 1.2, 12.0, 0.7, "garść", 50.0),
        p("Winogrona", 69.0, 0.7, 18.0, 0.2, "garść", 80.0),
        p("Kiwi", 61.0, 1.1, 15.0, 0.5, "szt.", 75.0),
        p("Mango", 60.0, 0.8, 15.0, 0.4, "szt.", 250.0),
        p("Ananas", 50.0, 0.5, 13.0, 0.1, "plaster", 80.0),
        p("Arbuz", 30.0, 0.6, 7.6, 0.2, "plaster", 300.0),
        p("Sok z cytryny", 22.0, 0.4, 7.0, 0.2, "łyżka", 15.0),
        p("Rodzynki", 299.0, 3.1, 79.0, 0.5, "łyżka", 15.0),
        p("Daktyle suszone", 282.0, 2.5, 75.0, 0.4, "szt.", 8.0),
        p("Śliwki suszone", 240.0, 2.2, 64.0, 0.4, "szt.", 10.0),
        p("Sok pomarańczowy", 45.0, 0.7, 10.4, 0.2, "szklanka", 250.0),

        // Orzechy, nasiona i tłuszcze
        p("Orzechy włoskie", 654.0, 15.0, 14.0, 65.0, "garść", 30.0),
        p("Migdały", 579.0, 21.0, 22.0, 50.0, "garść", 30.0),
        p("Orzechy nerkowca", 553.0, 18.0, 30.0, 44.0, "garść", 30.0),
        p("Orzeszki ziemne", 567.0, 26.0, 16.0, 49.0, "garść", 30.0),
        p("Masło orzechowe", 588.0, 25.0, 20.0, 50.0, "łyżka", 15.0),
        p("Nasiona chia", 486.0, 17.0, 42.0, 31.0, "łyżka", 10.0),
        p("Siemię lniane", 534.0, 18.0, 29.0, 42.0, "łyżka", 10.0),
        p("Pestki dyni", 559.0, 30.0, 11.0, 49.0, "łyżka", 10.0),
        p("Słonecznik łuskany", 584.0, 21.0, 20.0, 51.0, "łyżka", 10.0),
        p("Wiórki kokosowe", 660.0, 6.9, 24.0, 64.0, "łyżka", 6.0),
        p("Oliwa z oliwek", 884.0, 0.0, 0.0, 100.0, "łyżka", 10.0),
        p("Olej rzepakowy", 884.0, 0.0, 0.0, 100.0, "łyżka", 10.0),
        p("Olej kokosowy", 862.0, 0.0, 0.0, 100.0, "łyżka", 10.0),

        // Dodatki, sosy i słodkie
        p("Miód", 304.0, 0.3, 82.0, 0.0, "łyżeczka", 12.0),
        p("Cukier", 400.0, 0.0, 100.0, 0.0, "łyżeczka", 5.0),
        p("Syrop klonowy", 260.0, 0.0, 67.0, 0.1, "łyżka", 20.0),
        p("Dżem niskosłodzony", 150.0, 0.4, 37.0, 0.1, "łyżka", 20.0),
        p("Czekolada gorzka 70%", 580.0, 7.8, 46.0, 41.0, "kostka", 6.0),
        p("Kakao naturalne", 330.0, 22.0, 15.0, 11.0, "łyżeczka", 5.0),
        p("Odżywka białkowa (WPC)", 390.0, 75.0, 8.0, 6.0, "miarka", 30.0),
        p("Ketchup", 112.0, 1.2, 26.0, 0.1, "łyżka", 15.0),
        p("Majonez", 680.0, 1.0, 2.0, 75.0, "łyżka", 15.0),
        p("Musztarda", 66.0, 4.4, 5.8, 3.3, "łyżeczka", 5.0),
        p("Sos sojowy", 53.0, 8.0, 5.0, 0.1, "łyżka", 15.0),
        p("Pesto bazyliowe", 450.0, 5.0, 6.0, 45.0, "łyżka", 15.0)
    )

    /** Tekst bez polskich znaków i wielkich liter – do wyszukiwania („jablko” znajdzie „Jabłko”). */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('ł', 'l')
            .trim()
}
