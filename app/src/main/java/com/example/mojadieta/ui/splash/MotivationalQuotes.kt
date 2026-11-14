package com.example.mojadieta.ui.splash

import android.content.Context
import androidx.core.content.edit

/** 100 krótkich cytatów motywujących wyświetlanych na ekranie ładowania. */
object MotivationalQuotes {

    val all: List<String> = listOf(
        "Każdy zdrowy posiłek to głos oddany na lepszą wersję siebie.",
        "Nie musisz być idealna. Wystarczy, że jesteś konsekwentna.",
        "Małe kroki każdego dnia dają wielkie zmiany po miesiącach.",
        "Dbanie o siebie to nie egoizm, to fundament.",
        "Twoje ciało słyszy wszystko, co mówi Twój talerz.",
        "Dziś jest dobry dzień, żeby zacząć od nowa.",
        "Postęp, nie perfekcja.",
        "Szklanka wody to najprostszy prezent dla Twojego ciała.",
        "Nie licz dni. Spraw, żeby dni się liczyły.",
        "Zdrowe nawyki budujesz tak jak dom – cegła po cegle.",
        "Jedz kolorowo, żyj radośnie.",
        "Najtrudniejszy jest pierwszy krok. Ten masz już za sobą.",
        "Motywacja pomaga zacząć, nawyk pozwala wytrwać.",
        "Jesteś warta wysiłku, który w siebie wkładasz.",
        "Jeden gorszy posiłek nie przekreśla dobrego tygodnia.",
        "Zmiana zaczyna się w głowie, a kończy na talerzu.",
        "Twoje przyszłe ja podziękuje Ci za dzisiejszy wybór.",
        "Nie porównuj się z innymi. Porównuj się z sobą sprzed miesiąca.",
        "Zdrowie to bogactwo, którego nie da się kupić.",
        "Każdy dzień to nowa szansa na dobry wybór.",
        "Słuchaj swojego ciała – ono wie więcej, niż myślisz.",
        "Cierpliwość to też składnik zdrowej diety.",
        "Lepiej powoli do przodu niż szybko w miejscu.",
        "Planowanie posiłków to troska o siebie zapisana z wyprzedzeniem.",
        "Nie odkładaj na jutro zdrowia, które możesz mieć dziś.",
        "Śniadanie to pierwszy dobry wybór dnia.",
        "Twoje ciało to jedyne miejsce, w którym naprawdę mieszkasz.",
        "Siła nie pochodzi z tego, co potrafisz, ale z tego, czego się nie poddajesz.",
        "Warzywa na talerzu, uśmiech na twarzy.",
        "Każda wypita szklanka wody przybliża Cię do celu.",
        "Nie szukaj wymówek. Szukaj sposobów.",
        "Dobre samopoczucie smakuje lepiej niż jakikolwiek deser.",
        "Konsekwencja wygrywa z intensywnością.",
        "Jesteś bliżej celu niż wczoraj.",
        "Zdrowe jedzenie to nie kara, to nagroda dla ciała.",
        "Uwierz w siebie choć trochę bardziej niż wczoraj.",
        "Nawyk to decyzja podjęta raz i powtarzana codziennie.",
        "Lekkość w ciele zaczyna się od lekkości w głowie.",
        "Nie musisz robić wszystkiego. Zrób dziś jedną dobrą rzecz.",
        "Twoja dieta to inwestycja, a nie wydatek.",
        "Odżywiaj ciało, a ono odwdzięczy się energią.",
        "Każdy posiłek to nowa okazja, żeby zadbać o siebie.",
        "Sukces to suma małych wysiłków powtarzanych dzień po dniu.",
        "Wybieraj jedzenie, które daje Ci siłę, a nie tylko sytość.",
        "Zmęczenie mija, duma zostaje.",
        "Zaczynaj każdy dzień z wdzięcznością i szklanką wody.",
        "Nie jesteś sama na tej drodze – masz siebie.",
        "Nie trzeba być wielką, żeby zacząć. Trzeba zacząć, żeby być wielką.",
        "Prawdziwa zmiana dzieje się po cichu, każdego dnia.",
        "Zdrowe wybory stają się łatwiejsze z każdym razem.",
        "Bądź dla siebie tak dobra, jak dla najlepszej przyjaciółki.",
        "Talerz pełen kolorów to talerz pełen zdrowia.",
        "Twoje cele są ważniejsze niż chwilowe zachcianki.",
        "Nie licz kalorii jak wrogów – traktuj je jak informację.",
        "Każdy dzień bez poddawania się to zwycięstwo.",
        "Wysypiaj się, pij wodę, jedz prawdziwe jedzenie.",
        "Najlepszy moment na zdrowy wybór jest teraz.",
        "Ruch to radość, nie obowiązek.",
        "Dyscyplina to pamiętanie, czego naprawdę chcesz.",
        "Nawet najdłuższa podróż zaczyna się od jednego posiłku.",
        "Nie oceniaj dnia po wadze, oceniaj go po wyborach.",
        "Kiedy chcesz się poddać, przypomnij sobie, dlaczego zaczęłaś.",
        "Dobre jedzenie to dobry nastrój.",
        "Twoje zdrowie to Twoja supermoc.",
        "Nie potrzebujesz poniedziałku, żeby zacząć.",
        "Mała zmiana dziś to duża różnica jutro.",
        "Zdrowa dieta to styl życia, a nie chwilowy projekt.",
        "Jesteś silniejsza niż Twoje wymówki.",
        "Kochaj swoje ciało na każdym etapie drogi.",
        "Wybór należy do Ciebie – i to jest piękne.",
        "Nawodnienie to pierwszy krok do lepszej energii.",
        "Uśmiechnij się do swojego talerza.",
        "Nie musisz biec. Wystarczy, że idziesz do przodu.",
        "Każdy dobry dzień zaczyna się od dobrego planu.",
        "Twoje nawyki piszą historię Twojego zdrowia.",
        "Jedz uważnie, żyj świadomie.",
        "Dziś zrób coś, za co jutro sobie podziękujesz.",
        "Ciało osiąga to, w co wierzy umysł.",
        "Nie ma złych dni – są dni, z których się uczymy.",
        "Wytrwałość zamienia marzenia w rzeczywistość.",
        "Zdrowe przekąski to sprytny sposób na dobry dzień.",
        "Każdy wybór się liczy, nawet ten najmniejszy.",
        "Rób to dla siebie, nie dla innych.",
        "Spokój, cierpliwość i dobra kolacja.",
        "Twoja energia zaczyna się w kuchni.",
        "Dbaj o siebie dziś, by cieszyć się zdrowiem jutro.",
        "Zrównoważony talerz to zrównoważone życie.",
        "Nie musisz być najlepsza. Bądź lepsza niż wczoraj.",
        "Dobre nawyki są jak procent składany – rosną z czasem.",
        "Odpoczynek też jest częścią sukcesu.",
        "Pij wodę, oddychaj głęboko, idź dalej.",
        "Twoje zdrowie zasługuje na pierwsze miejsce w planie dnia.",
        "Każda porcja warzyw to krok w dobrą stronę.",
        "Wierz w proces – efekty przyjdą.",
        "Siła rodzi się z codziennych, prostych decyzji.",
        "Jedz to, co Cię karmi – ciało i duszę.",
        "Nie zatrzymuj się, gdy jest trudno. Zatrzymaj się, gdy skończysz.",
        "Każdy dzień to strona Twojej historii zdrowia.",
        "Bądź dumna z każdego, nawet najmniejszego postępu.",
        "Zdrowe nawyki dziś to lepsze jutro."
    )

    /**
     * Zwraca kolejny cytat z potasowanej kolejki zapisanej w SharedPreferences.
     * Dzięki temu przy każdym uruchomieniu jest inny cytat, a powtórki pojawiają się
     * dopiero po wyświetleniu wszystkich.
     */
    fun next(context: Context): String {
        val prefs = context.getSharedPreferences("splash_quotes", Context.MODE_PRIVATE)
        var order = prefs.getString(KEY_ORDER, null)
            ?.split(',')
            ?.mapNotNull { it.toIntOrNull() }
            ?.filter { it in all.indices }
            .orEmpty()
        var position = prefs.getInt(KEY_POSITION, 0)

        if (order.size != all.size || position >= order.size) {
            val lastShown = order.getOrNull(position - 1)
            order = all.indices.shuffled()
            // Nie zaczynaj nowej kolejki od cytatu, który był pokazany ostatnio.
            if (lastShown != null && order.first() == lastShown) {
                order = order.drop(1) + lastShown
            }
            position = 0
        }

        val index = order[position]
        prefs.edit {
            putString(KEY_ORDER, order.joinToString(","))
            putInt(KEY_POSITION, position + 1)
        }
        return all[index]
    }

    private const val KEY_ORDER = "order"
    private const val KEY_POSITION = "position"
}
