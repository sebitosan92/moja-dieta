# Moja Dieta — by Tala

Natywna aplikacja dietetyczna na Androida: dziennik posiłków z licznikiem wody, baza przepisów, tygodniowy planer posiłków i notatnik. Wszystkie dane są przechowywane lokalnie na telefonie.

## Instalacja na telefonie

1. Wejdź w zakładkę **[Releases](../../releases/latest)** i pobierz plik `moja-dieta-X.Y.Z.apk`.
2. Otwórz pobrany plik na telefonie i zezwól na instalację z tego źródła.
3. Kolejne wersje aplikacja wykryje sama: pokaże okno „Dostępna nowa wersja” i wyśle powiadomienie. Możesz też sprawdzić ręcznie: **Dziennik → ⓘ → Sprawdź aktualizacje**.

## Funkcje

- **Dziennik** — kalendarz tygodniowy z wyborem dowolnego dnia, podsumowanie kalorii i makro (B/W/T) względem dziennego celu, posiłki według pory dnia z liczbą porcji i zdjęciem, licznik wody, karta wagi.
- **Profil i cele** — kalkulator zapotrzebowania (wzór Mifflina-St Jeora, aktywność, cel: redukcja / utrzymanie / masa) albo cele ustawione ręcznie.
- **Przepisy** — wyszukiwarka, kategorie, zdjęcia z kadrowaniem, składniki z gramaturą i **automatyczne liczenie kalorii i makro** (na porcję), dodawanie do dziennika lub planera.
- **Baza produktów** — ponad 350 popularnych produktów, własne produkty, **skaner kodów kreskowych** i wyszukiwanie w [Open Food Facts](https://world.openfoodfacts.org) (dane na licencji ODbL).
- **Import przepisów** — z plików PDF, zdjęć i zrzutów ekranu (rozpoznawanie tekstu na telefonie); jadłospisy z podziałem na dni trafiają od razu do Planera.
- **Planer** — widok tygodnia, planowanie przepisów, kopiowanie dnia lub tygodnia do dziennika.
- **Waga** — ręczne pomiary, wykres i zmiana z 7/30 dni, automatyczny odczyt z **Health Connect** (np. waga Huawei przez Huawei Zdrowie).
- **Widżet** — kalorie dnia i licznik wody z przyciskami +250/+500 ml.
- **Notatnik** — listy zakupów, pomiary, samopoczucie.
- **Aktualizacje** — z GitHub Releases, z powiadomieniem o nowej wersji (sprawdzanie w tle co 12 h).

## Technologie

Kotlin · Jetpack Compose · Material 3 · Room · Navigation Compose · WorkManager · CameraX · ML Kit · Glance · Health Connect · Coil · MVVM + Clean Architecture.

## Źródła danych i licencje

- Dane produktów ze skanera i wyszukiwania online: [Open Food Facts](https://world.openfoodfacts.org), licencja [Open Database License (ODbL)](https://opendatacommons.org/licenses/odbl/1-0/).
- Biblioteki open source na licencji [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0): Kotlin, Kotlin Coroutines, Android Jetpack (Compose, Material 3, Material Icons, Navigation, Lifecycle, Room, WorkManager, CameraX, Glance, Health Connect, Core, Activity, ExifInterface), Coil.
- Skaner kodów i rozpoznawanie tekstu: Google ML Kit ([warunki](https://developers.google.com/ml-kit/terms)).
