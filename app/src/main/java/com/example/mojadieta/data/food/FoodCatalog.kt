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
        unitGrams: Double? = null,
        group: String? = null
    ) = FoodProduct(name, kcal, protein, carbs, fat, unit, unitGrams, group = group)

    val products: List<FoodProduct> = listOf(
        // Pieczywo i zboża
        p("Chleb żytni razowy", 213.0, 5.9, 41.2, 1.8, "kromka", 35.0, group = "Pieczywo i zboża"),
        p("Chleb pszenny", 260.0, 8.5, 50.0, 3.0, "kromka", 30.0, group = "Pieczywo i zboża"),
        p("Chleb pełnoziarnisty", 245.0, 9.0, 43.0, 3.5, "kromka", 35.0, group = "Pieczywo i zboża"),
        p("Bułka pszenna", 275.0, 9.0, 55.0, 2.5, "szt.", 50.0, group = "Pieczywo i zboża"),
        p("Bułka grahamka", 250.0, 9.0, 47.0, 3.0, "szt.", 60.0, group = "Pieczywo i zboża"),
        p("Tortilla pszenna", 310.0, 8.5, 52.0, 7.5, "szt.", 60.0, group = "Pieczywo i zboża"),
        p("Wafle ryżowe", 387.0, 8.0, 81.0, 2.8, "szt.", 10.0, group = "Pieczywo i zboża"),
        p("Płatki owsiane", 372.0, 13.5, 58.7, 7.0, "łyżka", 10.0, group = "Pieczywo i zboża"),
        p("Płatki kukurydziane", 380.0, 7.0, 84.0, 1.0, "szklanka", 30.0, group = "Pieczywo i zboża"),
        p("Musli bez cukru", 370.0, 10.0, 60.0, 8.0, "łyżka", 12.0, group = "Pieczywo i zboża"),
        p("Granola", 470.0, 10.0, 64.0, 20.0, "łyżka", 12.0, group = "Pieczywo i zboża"),
        p("Ryż biały (suchy)", 345.0, 6.7, 78.9, 0.7, "woreczek", 100.0, group = "Pieczywo i zboża"),
        p("Ryż brązowy (suchy)", 350.0, 7.5, 72.0, 2.7, "woreczek", 100.0, group = "Pieczywo i zboża"),
        p("Ryż basmati (suchy)", 350.0, 8.0, 77.0, 1.0, "woreczek", 100.0, group = "Pieczywo i zboża"),
        p("Ryż biały (ugotowany)", 130.0, 2.7, 28.0, 0.3, group = "Pieczywo i zboża"),
        p("Kasza gryczana (sucha)", 343.0, 13.0, 70.0, 3.4, "woreczek", 100.0, group = "Pieczywo i zboża"),
        p("Kasza jaglana (sucha)", 348.0, 10.5, 71.6, 2.9, "woreczek", 100.0, group = "Pieczywo i zboża"),
        p("Kasza bulgur (sucha)", 342.0, 12.0, 76.0, 1.3, "woreczek", 100.0, group = "Pieczywo i zboża"),
        p("Kasza kuskus (sucha)", 360.0, 12.8, 72.0, 0.6, group = "Pieczywo i zboża"),
        p("Komosa ryżowa (sucha)", 368.0, 14.0, 64.0, 6.0, group = "Pieczywo i zboża"),
        p("Makaron pszenny (suchy)", 360.0, 12.5, 72.0, 1.5, group = "Pieczywo i zboża"),
        p("Makaron pełnoziarnisty (suchy)", 345.0, 13.5, 66.0, 2.5, group = "Pieczywo i zboża"),
        p("Makaron (ugotowany)", 158.0, 5.8, 31.0, 0.9, group = "Pieczywo i zboża"),
        p("Mąka pszenna", 345.0, 10.0, 72.0, 1.2, "łyżka", 10.0, group = "Pieczywo i zboża"),
        p("Ziemniaki", 77.0, 2.0, 17.0, 0.1, "szt.", 100.0, group = "Pieczywo i zboża"),
        p("Bataty", 86.0, 1.6, 20.0, 0.1, "szt.", 200.0, group = "Pieczywo i zboża"),

        // Nabiał i jajka
        p("Jajko kurze", 140.0, 12.5, 0.7, 9.7, "szt.", 50.0, group = "Nabiał i jajka"),
        p("Białko jaja", 48.0, 10.9, 0.7, 0.2, "szt.", 33.0, group = "Nabiał i jajka"),
        p("Mleko 2%", 51.0, 3.4, 4.9, 2.0, "szklanka", 250.0, group = "Nabiał i jajka"),
        p("Mleko 3,2%", 61.0, 3.2, 4.8, 3.2, "szklanka", 250.0, group = "Nabiał i jajka"),
        p("Napój owsiany", 45.0, 1.0, 6.5, 1.5, "szklanka", 250.0, group = "Nabiał i jajka"),
        p("Napój sojowy", 40.0, 3.3, 2.5, 1.8, "szklanka", 250.0, group = "Nabiał i jajka"),
        p("Jogurt naturalny 2%", 61.0, 4.3, 6.2, 2.0, "kubek", 150.0, group = "Nabiał i jajka"),
        p("Jogurt grecki", 133.0, 4.0, 4.0, 10.0, "kubek", 150.0, group = "Nabiał i jajka"),
        p("Skyr naturalny", 63.0, 11.0, 4.0, 0.2, "kubek", 150.0, group = "Nabiał i jajka"),
        p("Kefir", 50.0, 3.4, 4.5, 2.0, "szklanka", 250.0, group = "Nabiał i jajka"),
        p("Twaróg półtłusty", 133.0, 18.7, 3.7, 4.7, "kostka", 250.0, group = "Nabiał i jajka"),
        p("Twaróg chudy", 99.0, 19.8, 3.5, 0.5, "kostka", 250.0, group = "Nabiał i jajka"),
        p("Serek wiejski", 97.0, 11.0, 2.0, 5.0, "opak.", 200.0, group = "Nabiał i jajka"),
        p("Serek twarogowy kanapkowy", 250.0, 7.0, 3.0, 23.0, "łyżka", 15.0, group = "Nabiał i jajka"),
        p("Ser żółty gouda", 356.0, 25.0, 0.0, 28.0, "plaster", 15.0, group = "Nabiał i jajka"),
        p("Mozzarella", 250.0, 18.0, 2.0, 19.0, "kulka", 125.0, group = "Nabiał i jajka"),
        p("Ser feta", 264.0, 14.0, 4.0, 21.0, group = "Nabiał i jajka"),
        p("Parmezan", 431.0, 38.0, 4.0, 29.0, "łyżka", 5.0, group = "Nabiał i jajka"),
        p("Ser camembert", 300.0, 20.0, 0.5, 24.0, group = "Nabiał i jajka"),
        p("Masło", 735.0, 0.7, 0.7, 82.0, "łyżeczka", 5.0, group = "Nabiał i jajka"),
        p("Śmietana 18%", 186.0, 2.5, 3.6, 18.0, "łyżka", 15.0, group = "Nabiał i jajka"),

        // Mięso, wędliny i ryby
        p("Pierś z kurczaka", 99.0, 21.5, 0.0, 1.3, "szt.", 200.0, group = "Mięso i ryby"),
        p("Udko z kurczaka bez skóry", 120.0, 19.0, 0.0, 5.0, "szt.", 100.0, group = "Mięso i ryby"),
        p("Pierś z indyka", 84.0, 19.2, 0.0, 1.0, group = "Mięso i ryby"),
        p("Mięso mielone wołowe", 250.0, 17.0, 0.0, 20.0, group = "Mięso i ryby"),
        p("Wołowina chuda", 113.0, 21.0, 0.0, 3.0, group = "Mięso i ryby"),
        p("Schab wieprzowy", 172.0, 21.0, 0.0, 9.6, group = "Mięso i ryby"),
        p("Polędwica sopocka", 103.0, 18.0, 1.0, 3.0, "plaster", 10.0, group = "Mięso i ryby"),
        p("Szynka z indyka", 95.0, 17.0, 1.5, 2.5, "plaster", 10.0, group = "Mięso i ryby"),
        p("Boczek", 350.0, 14.0, 0.0, 33.0, "plaster", 15.0, group = "Mięso i ryby"),
        p("Kiełbasa", 300.0, 14.0, 1.0, 27.0, group = "Mięso i ryby"),
        p("Łosoś świeży", 201.0, 20.0, 0.0, 13.6, group = "Mięso i ryby"),
        p("Łosoś wędzony", 162.0, 21.5, 0.0, 8.4, group = "Mięso i ryby"),
        p("Dorsz", 77.0, 17.7, 0.0, 0.7, group = "Mięso i ryby"),
        p("Pstrąg", 119.0, 20.5, 0.0, 4.0, group = "Mięso i ryby"),
        p("Tuńczyk w wodzie (puszka)", 103.0, 24.0, 0.0, 0.8, "puszka", 120.0, group = "Mięso i ryby"),
        p("Makrela wędzona", 220.0, 21.0, 0.0, 15.0, group = "Mięso i ryby"),
        p("Krewetki", 85.0, 19.0, 1.0, 1.0, group = "Mięso i ryby"),

        // Strączki i zamienniki mięsa
        p("Ciecierzyca (puszka)", 125.0, 7.5, 17.0, 2.5, "puszka", 240.0, group = "Strączki i zamienniki"),
        p("Fasola czerwona (puszka)", 100.0, 7.0, 14.0, 0.5, "puszka", 240.0, group = "Strączki i zamienniki"),
        p("Soczewica czerwona (sucha)", 330.0, 25.0, 50.0, 1.5, group = "Strączki i zamienniki"),
        p("Soczewica (ugotowana)", 116.0, 9.0, 20.0, 0.4, group = "Strączki i zamienniki"),
        p("Tofu naturalne", 120.0, 13.0, 2.0, 7.0, "kostka", 180.0, group = "Strączki i zamienniki"),
        p("Groszek zielony", 75.0, 5.5, 13.0, 0.4, group = "Strączki i zamienniki"),
        p("Edamame", 121.0, 12.0, 9.0, 5.0, group = "Strączki i zamienniki"),
        p("Hummus", 166.0, 7.9, 14.0, 9.6, "łyżka", 20.0, group = "Strączki i zamienniki"),

        // Warzywa
        p("Pomidor", 18.0, 0.9, 3.9, 0.2, "szt.", 150.0, group = "Warzywa"),
        p("Pomidorki koktajlowe", 18.0, 0.9, 3.9, 0.2, "szt.", 15.0, group = "Warzywa"),
        p("Ogórek", 15.0, 0.7, 3.6, 0.1, "szt.", 180.0, group = "Warzywa"),
        p("Papryka czerwona", 31.0, 1.0, 6.0, 0.3, "szt.", 180.0, group = "Warzywa"),
        p("Cebula", 40.0, 1.1, 9.3, 0.1, "szt.", 100.0, group = "Warzywa"),
        p("Czosnek", 149.0, 6.4, 33.0, 0.5, "ząbek", 5.0, group = "Warzywa"),
        p("Marchew", 41.0, 0.9, 9.6, 0.2, "szt.", 80.0, group = "Warzywa"),
        p("Brokuł", 34.0, 2.8, 6.6, 0.4, "różyczka", 20.0, group = "Warzywa"),
        p("Kalafior", 25.0, 1.9, 5.0, 0.3, group = "Warzywa"),
        p("Cukinia", 17.0, 1.2, 3.1, 0.3, "szt.", 250.0, group = "Warzywa"),
        p("Bakłażan", 25.0, 1.0, 6.0, 0.2, "szt.", 300.0, group = "Warzywa"),
        p("Szpinak", 23.0, 2.9, 3.6, 0.4, "garść", 30.0, group = "Warzywa"),
        p("Sałata", 15.0, 1.4, 2.9, 0.2, "liść", 10.0, group = "Warzywa"),
        p("Rukola", 25.0, 2.6, 3.7, 0.7, "garść", 20.0, group = "Warzywa"),
        p("Kapusta biała", 25.0, 1.3, 5.8, 0.1, group = "Warzywa"),
        p("Kapusta kiszona", 18.0, 1.0, 3.4, 0.2, group = "Warzywa"),
        p("Pieczarki", 22.0, 3.1, 3.3, 0.3, "szt.", 20.0, group = "Warzywa"),
        p("Kukurydza (puszka)", 85.0, 2.9, 15.0, 1.2, "łyżka", 20.0, group = "Warzywa"),
        p("Burak", 43.0, 1.6, 10.0, 0.2, "szt.", 150.0, group = "Warzywa"),
        p("Por", 61.0, 1.5, 14.0, 0.3, group = "Warzywa"),
        p("Seler naciowy", 16.0, 0.7, 3.0, 0.2, "łodyga", 40.0, group = "Warzywa"),
        p("Fasolka szparagowa", 31.0, 1.8, 7.0, 0.2, group = "Warzywa"),
        p("Dynia", 26.0, 1.0, 6.5, 0.1, group = "Warzywa"),
        p("Awokado", 160.0, 2.0, 8.5, 14.7, "szt.", 150.0, group = "Warzywa"),
        p("Oliwki zielone", 145.0, 1.0, 3.8, 15.0, "szt.", 4.0, group = "Warzywa"),
        p("Szczypiorek", 30.0, 3.3, 4.4, 0.7, "łyżka", 5.0, group = "Warzywa"),
        p("Natka pietruszki", 36.0, 3.0, 6.3, 0.8, "łyżka", 5.0, group = "Warzywa"),
        p("Passata pomidorowa", 30.0, 1.4, 5.5, 0.2, "szklanka", 250.0, group = "Warzywa"),
        p("Koncentrat pomidorowy", 82.0, 4.3, 19.0, 0.5, "łyżka", 20.0, group = "Warzywa"),

        // Owoce
        p("Jabłko", 52.0, 0.3, 14.0, 0.2, "szt.", 180.0, group = "Owoce"),
        p("Banan", 89.0, 1.1, 23.0, 0.3, "szt.", 120.0, group = "Owoce"),
        p("Pomarańcza", 47.0, 0.9, 12.0, 0.1, "szt.", 200.0, group = "Owoce"),
        p("Mandarynka", 53.0, 0.8, 13.0, 0.3, "szt.", 80.0, group = "Owoce"),
        p("Gruszka", 57.0, 0.4, 15.0, 0.1, "szt.", 180.0, group = "Owoce"),
        p("Truskawki", 32.0, 0.7, 7.7, 0.3, "garść", 80.0, group = "Owoce"),
        p("Borówki amerykańskie", 57.0, 0.7, 14.5, 0.3, "garść", 50.0, group = "Owoce"),
        p("Maliny", 52.0, 1.2, 12.0, 0.7, "garść", 50.0, group = "Owoce"),
        p("Winogrona", 69.0, 0.7, 18.0, 0.2, "garść", 80.0, group = "Owoce"),
        p("Kiwi", 61.0, 1.1, 15.0, 0.5, "szt.", 75.0, group = "Owoce"),
        p("Mango", 60.0, 0.8, 15.0, 0.4, "szt.", 250.0, group = "Owoce"),
        p("Ananas", 50.0, 0.5, 13.0, 0.1, "plaster", 80.0, group = "Owoce"),
        p("Arbuz", 30.0, 0.6, 7.6, 0.2, "plaster", 300.0, group = "Owoce"),
        p("Sok z cytryny", 22.0, 0.4, 7.0, 0.2, "łyżka", 15.0, group = "Owoce"),
        p("Rodzynki", 299.0, 3.1, 79.0, 0.5, "łyżka", 15.0, group = "Owoce"),
        p("Daktyle suszone", 282.0, 2.5, 75.0, 0.4, "szt.", 8.0, group = "Owoce"),
        p("Śliwki suszone", 240.0, 2.2, 64.0, 0.4, "szt.", 10.0, group = "Owoce"),
        p("Sok pomarańczowy", 45.0, 0.7, 10.4, 0.2, "szklanka", 250.0, group = "Owoce"),

        // Orzechy, nasiona i tłuszcze
        p("Orzechy włoskie", 654.0, 15.0, 14.0, 65.0, "garść", 30.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Migdały", 579.0, 21.0, 22.0, 50.0, "garść", 30.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Orzechy nerkowca", 553.0, 18.0, 30.0, 44.0, "garść", 30.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Orzeszki ziemne", 567.0, 26.0, 16.0, 49.0, "garść", 30.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Masło orzechowe", 588.0, 25.0, 20.0, 50.0, "łyżka", 15.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Nasiona chia", 486.0, 17.0, 42.0, 31.0, "łyżka", 10.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Siemię lniane", 534.0, 18.0, 29.0, 42.0, "łyżka", 10.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Pestki dyni", 559.0, 30.0, 11.0, 49.0, "łyżka", 10.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Słonecznik łuskany", 584.0, 21.0, 20.0, 51.0, "łyżka", 10.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Wiórki kokosowe", 660.0, 6.9, 24.0, 64.0, "łyżka", 6.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Oliwa z oliwek", 884.0, 0.0, 0.0, 100.0, "łyżka", 10.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Olej rzepakowy", 884.0, 0.0, 0.0, 100.0, "łyżka", 10.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Olej kokosowy", 862.0, 0.0, 0.0, 100.0, "łyżka", 10.0, group = "Orzechy, nasiona, tłuszcze"),

        // Dodatki, sosy i słodkie
        p("Miód", 304.0, 0.3, 82.0, 0.0, "łyżeczka", 12.0, group = "Przyprawy, sosy, dodatki"),
        p("Cukier", 400.0, 0.0, 100.0, 0.0, "łyżeczka", 5.0, group = "Przyprawy, sosy, dodatki"),
        p("Syrop klonowy", 260.0, 0.0, 67.0, 0.1, "łyżka", 20.0, group = "Przyprawy, sosy, dodatki"),
        p("Dżem niskosłodzony", 150.0, 0.4, 37.0, 0.1, "łyżka", 20.0, group = "Przyprawy, sosy, dodatki"),
        p("Czekolada gorzka 70%", 580.0, 7.8, 46.0, 41.0, "kostka", 6.0, group = "Przyprawy, sosy, dodatki"),
        p("Kakao naturalne", 330.0, 22.0, 15.0, 11.0, "łyżeczka", 5.0, group = "Przyprawy, sosy, dodatki"),
        p("Odżywka białkowa (WPC)", 390.0, 75.0, 8.0, 6.0, "miarka", 30.0, group = "Przyprawy, sosy, dodatki"),
        p("Ketchup", 112.0, 1.2, 26.0, 0.1, "łyżka", 15.0, group = "Przyprawy, sosy, dodatki"),
        p("Majonez", 680.0, 1.0, 2.0, 75.0, "łyżka", 15.0, group = "Przyprawy, sosy, dodatki"),
        p("Musztarda", 66.0, 4.4, 5.8, 3.3, "łyżeczka", 5.0, group = "Przyprawy, sosy, dodatki"),
        p("Sos sojowy", 53.0, 8.0, 5.0, 0.1, "łyżka", 15.0, group = "Przyprawy, sosy, dodatki"),
        p("Pesto bazyliowe", 450.0, 5.0, 6.0, 45.0, "łyżka", 15.0, group = "Przyprawy, sosy, dodatki"),
        // ---- Rozszerzenie bazy (wersja 1.3) ----
        // Pieczywo i zboża
        p("Chleb orkiszowy", 245.0, 9.5, 44.0, 2.5, "kromka", 35.0, group = "Pieczywo i zboża"),
        p("Chleb słonecznikowy", 270.0, 9.0, 40.0, 7.5, "kromka", 35.0, group = "Pieczywo i zboża"),
        p("Chleb bezglutenowy", 240.0, 3.5, 47.0, 4.0, "kromka", 30.0, group = "Pieczywo i zboża"),
        p("Bagietka", 270.0, 8.5, 55.0, 1.5, "kawałek", 50.0, group = "Pieczywo i zboża"),
        p("Bułka maślana", 330.0, 8.0, 55.0, 8.5, "szt.", 60.0, group = "Pieczywo i zboża"),
        p("Rogal", 300.0, 8.0, 52.0, 6.0, "szt.", 60.0, group = "Pieczywo i zboża"),
        p("Croissant", 406.0, 8.2, 45.8, 21.0, "szt.", 60.0, group = "Pieczywo i zboża"),
        p("Bajgiel", 257.0, 10.0, 50.0, 1.6, "szt.", 90.0, group = "Pieczywo i zboża"),
        p("Pieczywo chrupkie", 370.0, 10.0, 70.0, 2.5, "kromka", 10.0, group = "Pieczywo i zboża"),
        p("Bułka tarta", 350.0, 12.0, 70.0, 2.0, "łyżka", 10.0, group = "Pieczywo i zboża"),
        p("Płatki jaglane", 365.0, 11.0, 70.0, 3.5, "łyżka", 10.0, group = "Pieczywo i zboża"),
        p("Płatki żytnie", 340.0, 9.0, 65.0, 2.0, "łyżka", 10.0, group = "Pieczywo i zboża"),
        p("Otręby owsiane", 246.0, 17.0, 50.0, 7.0, "łyżka", 8.0, group = "Pieczywo i zboża"),
        p("Kasza manna (sucha)", 355.0, 10.0, 73.0, 1.0, "łyżka", 12.0, group = "Pieczywo i zboża"),
        p("Kasza pęczak (sucha)", 340.0, 10.0, 70.0, 1.5, group = "Pieczywo i zboża"),
        p("Kasza jęczmienna (sucha)", 345.0, 9.5, 72.0, 1.7, group = "Pieczywo i zboża"),
        p("Kasza gryczana (ugotowana)", 110.0, 4.0, 21.0, 1.0, group = "Pieczywo i zboża"),
        p("Kasza jaglana (ugotowana)", 120.0, 3.5, 23.0, 1.0, group = "Pieczywo i zboża"),
        p("Ryż brązowy (ugotowany)", 112.0, 2.6, 23.5, 0.9, group = "Pieczywo i zboża"),
        p("Kuskus (ugotowany)", 112.0, 3.8, 23.0, 0.2, group = "Pieczywo i zboża"),
        p("Komosa ryżowa (ugotowana)", 120.0, 4.4, 21.3, 1.9, group = "Pieczywo i zboża"),
        p("Makaron ryżowy (suchy)", 360.0, 6.0, 81.0, 0.6, group = "Pieczywo i zboża"),
        p("Makaron jajeczny (suchy)", 380.0, 14.0, 70.0, 4.0, group = "Pieczywo i zboża"),
        p("Gnocchi", 150.0, 3.5, 32.0, 0.5, group = "Pieczywo i zboża"),
        p("Mąka owsiana", 380.0, 13.0, 63.0, 7.0, "łyżka", 10.0, group = "Pieczywo i zboża"),
        p("Mąka żytnia", 330.0, 9.0, 70.0, 1.5, "łyżka", 10.0, group = "Pieczywo i zboża"),
        p("Mąka kukurydziana", 360.0, 7.0, 77.0, 3.0, "łyżka", 10.0, group = "Pieczywo i zboża"),
        p("Skrobia ziemniaczana", 350.0, 0.5, 86.0, 0.1, "łyżka", 10.0, group = "Pieczywo i zboża"),
        p("Ziemniaki gotowane", 75.0, 1.9, 16.5, 0.1, "szt.", 100.0, group = "Pieczywo i zboża"),
        p("Frytki z piekarnika", 160.0, 2.5, 25.0, 5.5, group = "Pieczywo i zboża"),
        p("Placki ziemniaczane", 230.0, 4.5, 26.0, 12.0, "szt.", 70.0, group = "Pieczywo i zboża"),

        // Nabiał
        p("Mleko 0,5%", 39.0, 3.4, 5.0, 0.5, "szklanka", 250.0, group = "Nabiał i jajka"),
        p("Mleko bez laktozy 2%", 50.0, 3.3, 4.8, 2.0, "szklanka", 250.0, group = "Nabiał i jajka"),
        p("Napój migdałowy", 24.0, 0.5, 3.0, 1.1, "szklanka", 250.0, group = "Nabiał i jajka"),
        p("Napój kokosowy", 20.0, 0.1, 2.7, 0.9, "szklanka", 250.0, group = "Nabiał i jajka"),
        p("Jogurt naturalny 0%", 38.0, 4.5, 5.0, 0.1, "kubek", 150.0, group = "Nabiał i jajka"),
        p("Jogurt owocowy", 95.0, 3.5, 14.0, 2.7, "kubek", 150.0, group = "Nabiał i jajka"),
        p("Jogurt proteinowy", 60.0, 10.0, 4.0, 0.2, "kubek", 200.0, group = "Nabiał i jajka"),
        p("Maślanka", 37.0, 3.3, 4.7, 0.5, "szklanka", 250.0, group = "Nabiał i jajka"),
        p("Serek homogenizowany waniliowy", 160.0, 7.0, 16.0, 7.5, "kubek", 150.0, group = "Nabiał i jajka"),
        p("Twaróg tłusty", 175.0, 16.0, 3.5, 10.5, "kostka", 250.0, group = "Nabiał i jajka"),
        p("Ser twarogowy kremowy (Philadelphia)", 230.0, 5.5, 4.0, 21.0, "łyżka", 20.0, group = "Nabiał i jajka"),
        p("Ser mascarpone", 430.0, 4.5, 3.5, 44.0, "łyżka", 20.0, group = "Nabiał i jajka"),
        p("Ser ricotta", 140.0, 9.0, 3.5, 10.0, "łyżka", 20.0, group = "Nabiał i jajka"),
        p("Serek mozzarella light", 165.0, 18.0, 1.5, 9.5, "kulka", 125.0, group = "Nabiał i jajka"),
        p("Ser edamski", 340.0, 26.0, 0.0, 26.0, "plaster", 15.0, group = "Nabiał i jajka"),
        p("Ser cheddar", 403.0, 25.0, 1.3, 33.0, "plaster", 15.0, group = "Nabiał i jajka"),
        p("Ser halloumi", 320.0, 21.0, 2.0, 25.0, group = "Nabiał i jajka"),
        p("Ser kozi", 300.0, 19.0, 1.0, 24.0, group = "Nabiał i jajka"),
        p("Ser topiony", 230.0, 11.0, 6.0, 18.0, "trójkącik", 17.0, group = "Nabiał i jajka"),
        p("Ser pleśniowy (lazur)", 350.0, 21.0, 1.0, 29.0, group = "Nabiał i jajka"),
        p("Śmietana 12%", 133.0, 2.8, 4.0, 12.0, "łyżka", 15.0, group = "Nabiał i jajka"),
        p("Śmietanka 30%", 290.0, 2.2, 3.2, 30.0, "łyżka", 15.0, group = "Nabiał i jajka"),
        p("Jajko na twardo", 155.0, 12.6, 1.1, 10.6, "szt.", 50.0, group = "Nabiał i jajka"),
        p("Masło klarowane", 900.0, 0.0, 0.0, 99.8, "łyżeczka", 5.0, group = "Nabiał i jajka"),
        p("Margaryna", 530.0, 0.2, 0.5, 59.0, "łyżeczka", 5.0, group = "Nabiał i jajka"),

        // Mięso i wędliny
        p("Filet z kurczaka pieczony", 165.0, 31.0, 0.0, 3.6, group = "Mięso i ryby"),
        p("Skrzydełka z kurczaka", 220.0, 18.0, 0.0, 16.0, "szt.", 90.0, group = "Mięso i ryby"),
        p("Wątróbka drobiowa", 120.0, 17.0, 1.0, 5.0, group = "Mięso i ryby"),
        p("Indyk mielony", 150.0, 19.0, 0.0, 8.0, group = "Mięso i ryby"),
        p("Wieprzowina mielona", 260.0, 17.0, 0.0, 21.0, group = "Mięso i ryby"),
        p("Karkówka", 235.0, 16.0, 0.0, 19.0, group = "Mięso i ryby"),
        p("Polędwica wieprzowa", 110.0, 21.0, 0.0, 3.0, group = "Mięso i ryby"),
        p("Żeberka wieprzowe", 290.0, 17.0, 0.0, 25.0, group = "Mięso i ryby"),
        p("Stek wołowy (antrykot)", 220.0, 19.0, 0.0, 16.0, group = "Mięso i ryby"),
        p("Cielęcina", 105.0, 20.0, 0.0, 2.5, group = "Mięso i ryby"),
        p("Kaczka (pierś bez skóry)", 135.0, 20.0, 0.0, 6.0, group = "Mięso i ryby"),
        p("Szynka wieprzowa gotowana", 120.0, 19.0, 1.0, 4.5, "plaster", 15.0, group = "Mięso i ryby"),
        p("Szynka parmeńska", 250.0, 27.0, 0.0, 15.0, "plaster", 10.0, group = "Mięso i ryby"),
        p("Salami", 400.0, 22.0, 1.5, 34.0, "plaster", 6.0, group = "Mięso i ryby"),
        p("Kabanosy", 450.0, 25.0, 1.0, 38.0, "szt.", 15.0, group = "Mięso i ryby"),
        p("Parówki", 260.0, 12.0, 2.0, 23.0, "szt.", 35.0, group = "Mięso i ryby"),
        p("Kiełbasa krakowska", 220.0, 20.0, 1.0, 15.0, "plaster", 10.0, group = "Mięso i ryby"),
        p("Pasztet", 280.0, 11.0, 5.0, 24.0, "łyżka", 20.0, group = "Mięso i ryby"),
        p("Pierś z kurczaka wędzona", 110.0, 22.0, 1.0, 2.0, "plaster", 10.0, group = "Mięso i ryby"),
        p("Kotlet schabowy panierowany", 250.0, 19.0, 10.0, 15.0, "szt.", 150.0, group = "Mięso i ryby"),

        // Ryby i owoce morza
        p("Łosoś pieczony", 206.0, 22.0, 0.0, 12.0, group = "Mięso i ryby"),
        p("Mintaj", 72.0, 16.0, 0.0, 0.8, group = "Mięso i ryby"),
        p("Tilapia", 96.0, 20.0, 0.0, 1.7, group = "Mięso i ryby"),
        p("Halibut", 110.0, 21.0, 0.0, 2.3, group = "Mięso i ryby"),
        p("Śledź w oleju", 260.0, 14.0, 1.0, 22.0, group = "Mięso i ryby"),
        p("Śledź marynowany", 190.0, 14.0, 6.0, 12.0, group = "Mięso i ryby"),
        p("Sardynki w oleju (puszka)", 210.0, 24.0, 0.0, 12.0, "puszka", 90.0, group = "Mięso i ryby"),
        p("Tuńczyk w oleju (puszka)", 190.0, 26.0, 0.0, 9.0, "puszka", 120.0, group = "Mięso i ryby"),
        p("Paluszki surimi", 95.0, 7.0, 14.0, 1.0, "szt.", 15.0, group = "Mięso i ryby"),
        p("Kalmary", 92.0, 15.6, 3.1, 1.4, group = "Mięso i ryby"),
        p("Paluszki rybne", 210.0, 12.0, 20.0, 9.0, "szt.", 28.0, group = "Mięso i ryby"),

        // Strączki
        p("Fasola biała (puszka)", 95.0, 6.5, 13.0, 0.4, "puszka", 240.0, group = "Strączki i zamienniki"),
        p("Fasola biała (sucha)", 290.0, 21.0, 46.0, 1.5, group = "Strączki i zamienniki"),
        p("Ciecierzyca (sucha)", 364.0, 19.0, 61.0, 6.0, group = "Strączki i zamienniki"),
        p("Soczewica zielona (sucha)", 340.0, 25.0, 52.0, 1.2, group = "Strączki i zamienniki"),
        p("Groch łuskany (suchy)", 330.0, 23.0, 52.0, 1.5, group = "Strączki i zamienniki"),
        p("Tofu wędzone", 160.0, 16.0, 2.0, 10.0, "kostka", 180.0, group = "Strączki i zamienniki"),
        p("Tempeh", 190.0, 19.0, 9.0, 11.0, group = "Strączki i zamienniki"),
        p("Napój sojowy bez cukru", 33.0, 3.3, 0.5, 1.8, "szklanka", 250.0, group = "Strączki i zamienniki"),

        // Warzywa
        p("Papryka żółta", 27.0, 1.0, 6.3, 0.2, "szt.", 180.0, group = "Warzywa"),
        p("Papryka zielona", 20.0, 0.9, 4.6, 0.2, "szt.", 160.0, group = "Warzywa"),
        p("Cebula czerwona", 40.0, 1.1, 9.3, 0.1, "szt.", 100.0, group = "Warzywa"),
        p("Szalotka", 72.0, 2.5, 17.0, 0.1, "szt.", 25.0, group = "Warzywa"),
        p("Ogórek kiszony", 12.0, 0.6, 2.2, 0.1, "szt.", 60.0, group = "Warzywa"),
        p("Rzodkiewka", 16.0, 0.7, 3.4, 0.1, "szt.", 10.0, group = "Warzywa"),
        p("Kalarepa", 27.0, 1.7, 6.2, 0.1, "szt.", 150.0, group = "Warzywa"),
        p("Brukselka", 43.0, 3.4, 9.0, 0.3, group = "Warzywa"),
        p("Kapusta czerwona", 31.0, 1.4, 7.4, 0.2, group = "Warzywa"),
        p("Kapusta pekińska", 13.0, 1.2, 2.2, 0.2, group = "Warzywa"),
        p("Jarmuż", 49.0, 4.3, 8.8, 0.9, "garść", 30.0, group = "Warzywa"),
        p("Seler korzeniowy", 42.0, 1.5, 9.2, 0.3, group = "Warzywa"),
        p("Pietruszka korzeń", 55.0, 2.3, 12.0, 0.5, "szt.", 80.0, group = "Warzywa"),
        p("Szparagi", 20.0, 2.2, 3.9, 0.1, "szt.", 20.0, group = "Warzywa"),
        p("Pomidory z puszki", 21.0, 1.0, 4.0, 0.2, "puszka", 400.0, group = "Warzywa"),
        p("Pomidory suszone w oleju", 213.0, 5.0, 23.0, 14.0, "szt.", 10.0, group = "Warzywa"),
        p("Mieszanka sałat", 17.0, 1.3, 3.3, 0.2, "garść", 30.0, group = "Warzywa"),
        p("Roszponka", 21.0, 2.0, 3.6, 0.4, "garść", 20.0, group = "Warzywa"),
        p("Kiełki", 30.0, 3.0, 5.0, 0.5, "garść", 20.0, group = "Warzywa"),
        p("Imbir", 80.0, 1.8, 18.0, 0.8, "łyżeczka", 5.0, group = "Warzywa"),
        p("Bazylia świeża", 23.0, 3.2, 2.7, 0.6, "garść", 5.0, group = "Warzywa"),
        p("Koperek", 43.0, 3.5, 7.0, 1.1, "łyżka", 5.0, group = "Warzywa"),
        p("Mrożona mieszanka warzyw", 50.0, 2.5, 9.0, 0.4, group = "Warzywa"),
        p("Warzywa na patelnię (mrożone)", 55.0, 2.5, 9.5, 0.8, group = "Warzywa"),
        p("Papryka pieczona (słoik)", 30.0, 1.0, 5.0, 0.3, group = "Warzywa"),

        // Owoce
        p("Brzoskwinia", 39.0, 0.9, 9.5, 0.3, "szt.", 150.0, group = "Owoce"),
        p("Nektarynka", 44.0, 1.1, 10.5, 0.3, "szt.", 150.0, group = "Owoce"),
        p("Morela", 48.0, 1.4, 11.0, 0.4, "szt.", 40.0, group = "Owoce"),
        p("Śliwki", 46.0, 0.7, 11.4, 0.3, "szt.", 30.0, group = "Owoce"),
        p("Wiśnie", 50.0, 1.0, 12.0, 0.3, "garść", 80.0, group = "Owoce"),
        p("Czereśnie", 63.0, 1.1, 16.0, 0.2, "garść", 80.0, group = "Owoce"),
        p("Jeżyny", 43.0, 1.4, 9.6, 0.5, "garść", 50.0, group = "Owoce"),
        p("Porzeczki", 56.0, 1.4, 13.8, 0.4, "garść", 50.0, group = "Owoce"),
        p("Grejpfrut", 42.0, 0.8, 10.7, 0.1, "szt.", 300.0, group = "Owoce"),
        p("Granat", 83.0, 1.7, 18.7, 1.2, "szt.", 250.0, group = "Owoce"),
        p("Melon", 34.0, 0.8, 8.2, 0.2, "plaster", 200.0, group = "Owoce"),
        p("Papaja", 43.0, 0.5, 11.0, 0.3, group = "Owoce"),
        p("Marakuja", 97.0, 2.2, 23.0, 0.7, "szt.", 20.0, group = "Owoce"),
        p("Limonka (sok)", 25.0, 0.4, 8.4, 0.1, "łyżka", 15.0, group = "Owoce"),
        p("Owoce leśne mrożone", 45.0, 1.0, 10.0, 0.4, "garść", 50.0, group = "Owoce"),
        p("Morele suszone", 241.0, 3.4, 63.0, 0.5, "szt.", 8.0, group = "Owoce"),
        p("Żurawina suszona", 308.0, 0.1, 82.0, 1.1, "łyżka", 10.0, group = "Owoce"),
        p("Figi suszone", 249.0, 3.3, 64.0, 0.9, "szt.", 15.0, group = "Owoce"),
        p("Mus owocowy (saszetka)", 60.0, 0.4, 14.0, 0.1, "saszetka", 90.0, group = "Owoce"),

        // Orzechy i tłuszcze
        p("Orzechy laskowe", 628.0, 15.0, 17.0, 61.0, "garść", 30.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Pistacje", 560.0, 20.0, 28.0, 45.0, "garść", 30.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Orzechy pekan", 691.0, 9.2, 14.0, 72.0, "garść", 30.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Orzechy brazylijskie", 656.0, 14.0, 12.0, 66.0, "szt.", 5.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Płatki migdałowe", 579.0, 21.0, 22.0, 50.0, "łyżka", 8.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Sezam", 573.0, 18.0, 23.0, 50.0, "łyżka", 9.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Tahini", 595.0, 17.0, 21.0, 54.0, "łyżka", 15.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Krem orzechowy z kakao", 540.0, 6.0, 57.0, 31.0, "łyżka", 15.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Olej lniany", 884.0, 0.0, 0.0, 100.0, "łyżka", 10.0, group = "Orzechy, nasiona, tłuszcze"),
        p("Olej słonecznikowy", 884.0, 0.0, 0.0, 100.0, "łyżka", 10.0, group = "Orzechy, nasiona, tłuszcze"),

        // Słodycze i przekąski
        p("Czekolada mleczna", 535.0, 7.5, 59.0, 30.0, "kostka", 6.0, group = "Słodycze i przekąski"),
        p("Czekolada biała", 540.0, 6.0, 59.0, 31.0, "kostka", 6.0, group = "Słodycze i przekąski"),
        p("Baton czekoladowy", 480.0, 5.0, 62.0, 23.0, "szt.", 50.0, group = "Słodycze i przekąski"),
        p("Baton proteinowy", 370.0, 30.0, 35.0, 12.0, "szt.", 55.0, group = "Słodycze i przekąski"),
        p("Baton musli", 420.0, 6.0, 65.0, 15.0, "szt.", 25.0, group = "Słodycze i przekąski"),
        p("Herbatniki", 440.0, 7.0, 75.0, 12.0, "szt.", 7.0, group = "Słodycze i przekąski"),
        p("Ciastka owsiane", 460.0, 6.0, 66.0, 19.0, "szt.", 15.0, group = "Słodycze i przekąski"),
        p("Wafelek", 520.0, 6.0, 60.0, 28.0, "szt.", 30.0, group = "Słodycze i przekąski"),
        p("Lody śmietankowe", 210.0, 3.5, 24.0, 11.0, "gałka", 50.0, group = "Słodycze i przekąski"),
        p("Sernik", 320.0, 7.0, 28.0, 20.0, "kawałek", 120.0, group = "Słodycze i przekąski"),
        p("Szarlotka", 270.0, 3.0, 40.0, 11.0, "kawałek", 120.0, group = "Słodycze i przekąski"),
        p("Pączek", 400.0, 6.0, 50.0, 19.0, "szt.", 70.0, group = "Słodycze i przekąski"),
        p("Chipsy ziemniaczane", 540.0, 6.5, 52.0, 34.0, "garść", 25.0, group = "Słodycze i przekąski"),
        p("Paluszki słone", 380.0, 10.0, 75.0, 4.0, "garść", 20.0, group = "Słodycze i przekąski"),
        p("Popcorn", 390.0, 11.0, 63.0, 10.0, "garść", 15.0, group = "Słodycze i przekąski"),
        p("Żelki", 340.0, 6.0, 77.0, 0.2, "garść", 20.0, group = "Słodycze i przekąski"),
        p("Chałwa", 500.0, 12.0, 50.0, 30.0, group = "Słodycze i przekąski"),
        p("Krakersy", 480.0, 9.0, 66.0, 20.0, "szt.", 5.0, group = "Słodycze i przekąski"),

        // Napoje
        p("Sok jabłkowy", 46.0, 0.1, 11.0, 0.1, "szklanka", 250.0, group = "Napoje"),
        p("Sok multiwitamina", 50.0, 0.3, 12.0, 0.1, "szklanka", 250.0, group = "Napoje"),
        p("Napój gazowany (cola)", 42.0, 0.0, 10.6, 0.0, "puszka", 330.0, group = "Napoje"),
        p("Napój gazowany zero", 0.5, 0.0, 0.1, 0.0, "puszka", 330.0, group = "Napoje"),
        p("Napój izotoniczny", 26.0, 0.0, 6.4, 0.0, "butelka", 500.0, group = "Napoje"),
        p("Kawa czarna", 2.0, 0.1, 0.0, 0.0, "filiżanka", 200.0, group = "Napoje"),
        p("Cappuccino", 40.0, 2.0, 3.5, 2.0, "kubek", 250.0, group = "Napoje"),
        p("Latte", 45.0, 2.5, 4.0, 2.0, "kubek", 300.0, group = "Napoje"),
        p("Herbata bez cukru", 1.0, 0.0, 0.2, 0.0, "kubek", 250.0, group = "Napoje"),
        p("Kakao na mleku", 75.0, 3.5, 10.0, 2.3, "kubek", 250.0, group = "Napoje"),
        p("Koktajl proteinowy (gotowy)", 60.0, 8.0, 5.0, 1.0, "butelka", 330.0, group = "Napoje"),
        p("Piwo", 43.0, 0.5, 3.6, 0.0, "puszka", 500.0, group = "Napoje"),
        p("Piwo bezalkoholowe", 25.0, 0.3, 5.5, 0.0, "puszka", 500.0, group = "Napoje"),
        p("Wino czerwone", 85.0, 0.1, 2.6, 0.0, "kieliszek", 150.0, group = "Napoje"),

        // Dania gotowe i fast food
        p("Pierogi ruskie", 210.0, 6.5, 33.0, 5.5, "szt.", 40.0, group = "Dania gotowe"),
        p("Pierogi z mięsem", 230.0, 9.0, 30.0, 8.0, "szt.", 40.0, group = "Dania gotowe"),
        p("Naleśnik z serem", 210.0, 9.0, 25.0, 8.0, "szt.", 120.0, group = "Dania gotowe"),
        p("Gołąbki w sosie pomidorowym", 120.0, 6.0, 10.0, 6.0, "szt.", 200.0, group = "Dania gotowe"),
        p("Bigos", 130.0, 6.0, 6.0, 9.0, group = "Dania gotowe"),
        p("Zupa pomidorowa z makaronem", 60.0, 1.8, 9.0, 1.8, "talerz", 350.0, group = "Dania gotowe"),
        p("Rosół z makaronem", 45.0, 2.5, 5.0, 1.5, "talerz", 350.0, group = "Dania gotowe"),
        p("Żurek", 75.0, 3.0, 5.0, 4.5, "talerz", 350.0, group = "Dania gotowe"),
        p("Krem z dyni", 55.0, 1.0, 7.0, 2.5, "talerz", 300.0, group = "Dania gotowe"),
        p("Pizza margherita", 250.0, 10.0, 33.0, 9.0, "kawałek", 110.0, group = "Dania gotowe"),
        p("Hamburger", 250.0, 13.0, 28.0, 10.0, "szt.", 200.0, group = "Dania gotowe"),
        p("Kebab w bułce", 230.0, 12.0, 22.0, 11.0, "szt.", 350.0, group = "Dania gotowe"),
        p("Sushi (maki)", 145.0, 5.0, 28.0, 1.0, "szt.", 25.0, group = "Dania gotowe"),
        p("Spaghetti bolognese", 150.0, 7.5, 18.0, 5.0, "talerz", 350.0, group = "Dania gotowe"),
        p("Lasagne", 165.0, 9.0, 14.0, 8.0, "porcja", 300.0, group = "Dania gotowe"),
        p("Sałatka grecka", 100.0, 3.0, 4.0, 8.0, "miska", 250.0, group = "Dania gotowe"),
        p("Owsianka na mleku (gotowa)", 110.0, 4.5, 16.0, 3.0, "miska", 300.0, group = "Dania gotowe"),

        // Dodatki, sosy, przyprawy
        p("Sos czosnkowy", 300.0, 1.5, 6.0, 30.0, "łyżka", 15.0, group = "Przyprawy, sosy, dodatki"),
        p("Sos pomidorowy do makaronu", 50.0, 1.5, 8.0, 1.5, "słoik", 500.0, group = "Przyprawy, sosy, dodatki"),
        p("Sos barbecue", 170.0, 1.0, 40.0, 0.5, "łyżka", 15.0, group = "Przyprawy, sosy, dodatki"),
        p("Sriracha", 93.0, 1.9, 19.0, 0.9, "łyżeczka", 5.0, group = "Przyprawy, sosy, dodatki"),
        p("Ocet balsamiczny", 88.0, 0.5, 17.0, 0.0, "łyżka", 15.0, group = "Przyprawy, sosy, dodatki"),
        p("Chrzan", 70.0, 1.5, 11.0, 1.5, "łyżeczka", 5.0, group = "Przyprawy, sosy, dodatki"),
        p("Ketchup bez cukru", 40.0, 1.5, 7.0, 0.3, "łyżka", 15.0, group = "Przyprawy, sosy, dodatki"),
        p("Majonez light", 330.0, 1.0, 8.0, 33.0, "łyżka", 15.0, group = "Przyprawy, sosy, dodatki"),
        p("Guacamole", 155.0, 2.0, 8.0, 14.0, "łyżka", 20.0, group = "Przyprawy, sosy, dodatki"),
        p("Erytrytol", 0.0, 0.0, 100.0, 0.0, "łyżeczka", 5.0, group = "Przyprawy, sosy, dodatki"),
        p("Ksylitol", 240.0, 0.0, 100.0, 0.0, "łyżeczka", 5.0, group = "Przyprawy, sosy, dodatki"),
        p("Cukier puder", 400.0, 0.0, 100.0, 0.0, "łyżeczka", 4.0, group = "Przyprawy, sosy, dodatki"),
        p("Drożdże świeże", 105.0, 12.0, 11.0, 1.5, group = "Przyprawy, sosy, dodatki"),
        p("Proszek do pieczenia", 50.0, 0.0, 25.0, 0.0, "łyżeczka", 4.0, group = "Przyprawy, sosy, dodatki"),
        p("Bulion (kostka)", 230.0, 10.0, 15.0, 15.0, "szt.", 10.0, group = "Przyprawy, sosy, dodatki"),
        p("Odżywka białkowa wegańska", 380.0, 70.0, 10.0, 6.0, "miarka", 30.0, group = "Przyprawy, sosy, dodatki")
    )

    /** Działy sklepu w kolejności na liście zakupów. */
    val GROUP_ORDER = listOf(
        "Warzywa", "Owoce", "Pieczywo i zboża", "Nabiał i jajka", "Mięso i ryby",
        "Strączki i zamienniki", "Orzechy, nasiona, tłuszcze", "Przyprawy, sosy, dodatki",
        "Słodycze i przekąski", "Napoje", "Dania gotowe", OTHER_GROUP
    )

    const val OTHER_GROUP = "Inne"

    private val byName: Map<String, FoodProduct> by lazy { products.associateBy { normalize(it.name) } }

    /** Dział sklepu dla nazwy składnika: dokładne dopasowanie, potem najdłuższa nazwa z bazy zawarta w tekście. */
    fun groupFor(name: String): String {
        val key = normalize(name)
        byName[key]?.group?.let { return it }
        val match = products
            .filter { it.group != null }
            .map { normalize(it.name) to it.group!! }
            .filter { (n, _) -> key.contains(n) || n.contains(key) && key.length >= 4 }
            .maxByOrNull { it.first.length }
        if (match != null) return match.second
        // Pierwsze słowo nazwy (np. „Pomidor koktajlowy” → „pomidor”).
        val first = key.split(' ').firstOrNull().orEmpty()
        if (first.length >= 4) {
            products.firstOrNull { it.group != null && normalize(it.name).startsWith(first.take(first.length - 1)) }
                ?.group?.let { return it }
        }
        return OTHER_GROUP
    }

    /** Tekst bez polskich znaków i wielkich liter – do wyszukiwania („jablko” znajdzie „Jabłko”). */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('ł', 'l')
            .trim()
}
