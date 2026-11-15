# Moja Dieta — by Tala

Natywna aplikacja dietetyczna na Androida: dziennik posiłków z licznikiem wody, baza przepisów, tygodniowy planer z listą zakupów, aktywność, waga i notatnik. Wszystkie dane są przechowywane lokalnie na telefonie.

## Instalacja na telefonie

1. Wejdź w zakładkę **[Releases](../../releases/latest)** i pobierz plik `moja-dieta-X.Y.Z.apk`.
2. Otwórz pobrany plik na telefonie i zezwól na instalację z tego źródła.
3. Kolejne wersje aplikacja wykryje sama: pokaże okno „Dostępna nowa wersja” i wyśle powiadomienie. Możesz też sprawdzić ręcznie: **Dziennik → ⋮ → O aplikacji → Sprawdź aktualizacje**.

## Funkcje

- **Dziennik** — kalendarz tygodniowy z kolorowymi kropkami (zielona: w celu, czerwona: ponad cel), podsumowanie kalorii i makro względem celu, błonnik/cukry/sól, posiłki według pory dnia z porcjami i zdjęciem, licznik wody, karta wagi.
  - przesunięcie wpisu w prawo = zjedzone, w lewo = usuń (z „Cofnij”), „Zaznacz wszystkie”, „Powtórz z wczoraj” (pora posiłku albo cały dzień), kopiowanie wpisu na inny dzień, edycja wpisów ręcznych,
  - ostatnio używane i ulubione produkty na górze listy.
- **Profil i cele** — kalkulator zapotrzebowania (wzór Mifflina-St Jeora, aktywność, cel) albo cele ustawione ręcznie.
- **Przepisy** — wyszukiwarka, kategorie, ulubione, tagi z filtrami, zdjęcia z kadrowaniem, składniki z gramaturą i automatyczne liczenie kalorii, makro, błonnika, cukrów i soli, udostępnianie przepisu (tekst + zdjęcie).
- **Baza produktów** — ponad 350 popularnych produktów, własne produkty, skaner kodów kreskowych i wyszukiwanie w [Open Food Facts](https://world.openfoodfacts.org) (dane na licencji ODbL).
- **Import przepisów** — z plików PDF, zdjęć i zrzutów ekranu; jadłospisy z podziałem na dni trafiają od razu do Planera.
- **Planer** — posiłki i treningi na cały tydzień, kopiowanie do dziennika, **lista zakupów** tworzona z zaplanowanych przepisów (zsumowane gramatury, podział na działy sklepu, odhaczanie, własne pozycje, udostępnianie).
- **Aktywność** — rower, rower stacjonarny (także z mocą w watach), spacer, bieg, nordic walking, pływanie, siłownia i inne; szacowanie spalonych kalorii (MET), ulubione treningi, kroki i cel kroków, wykres tygodnia, cel WHO 150 min, seria dni. Treningi, kroki, dystans i tętno z opaski lub zegarka przez **Health Connect** (np. Samsung Health). Spalone kalorie mają charakter informacyjny i nie zmieniają celu diety.
- **Waga i ciało** — pomiary wagi z wykresem, skład ciała (tkanka tłuszczowa, masa beztłuszczowa, woda, masa kostna) i obwody (talia, biodra, klatka, udo, ramię); odczyt z Health Connect (np. waga Huawei przez Huawei Zdrowie).
- **Statystyki** — 7 i 30 dni: średnie kalorie i makro, dni w celu, woda, zmiana wagi, aktywność, kroki.
- **Przypomnienia** — ważenie (pomija dzień z pomiarem), woda (tylko poniżej celu), posiłki (tylko bez wpisu), planowanie tygodnia, niedzielne podsumowanie tygodnia.
- **Post przerywany** — schematy 12:12–20:4, licznik w dzienniku i powiadomienia o oknie jedzenia.
- **Wygląd** — motyw jasny, ciemny albo jak w telefonie; ekran startowy z cytatem raz dziennie, zawsze, krótki albo wyłączony.
- **Kopia zapasowa** — cały stan aplikacji w jednym pliku ZIP (można zapisać np. na Dysku Google) i przywracanie.
- **Widżety i skróty** — widżet dnia (kalorie, woda, waga, kroki), mały widżet wody, kafelek „Woda +250 ml” w szybkich ustawieniach, skróty po przytrzymaniu ikony (+250 ml, skanuj kod, dodaj wagę, dodaj trening).
- **Notatnik** — notatki, pomiary, samopoczucie.
- **Aktualizacje** — z GitHub Releases, z powiadomieniem o nowej wersji.

## Technologie

Kotlin · Jetpack Compose · Material 3 · Room · Navigation Compose · WorkManager · CameraX · ML Kit · Glance · Health Connect · Coil · MVVM + Clean Architecture.

## Źródła danych i licencje

- Dane produktów ze skanera i wyszukiwania online: [Open Food Facts](https://world.openfoodfacts.org), licencja [Open Database License (ODbL)](https://opendatacommons.org/licenses/odbl/1-0/).
- Biblioteki open source na licencji [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0): Kotlin, Kotlin Coroutines, Android Jetpack (Compose, Material 3, Material Icons, Navigation, Lifecycle, Room, WorkManager, CameraX, Glance, Health Connect, Core, Activity, ExifInterface), Coil.
- Skaner kodów i rozpoznawanie tekstu: Google ML Kit ([warunki](https://developers.google.com/ml-kit/terms)).
