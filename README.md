# Moja Dieta — by Tala

Natywna aplikacja dietetyczna na Androida: dziennik posiłków z licznikiem wody, baza przepisów, tygodniowy planer posiłków i notatnik. Wszystkie dane są przechowywane lokalnie na telefonie.

## Instalacja na telefonie

1. Wejdź w zakładkę **[Releases](../../releases/latest)** i pobierz plik `moja-dieta-X.Y.Z.apk`.
2. Otwórz pobrany plik na telefonie i zezwól na instalację z tego źródła.
3. Kolejne wersje aplikacja wykryje sama: pokaże okno „Dostępna nowa wersja” i wyśle powiadomienie. Możesz też sprawdzić ręcznie: **Dziennik → ⓘ → Sprawdź aktualizacje**.

## Funkcje

- **Dziennik** — kalendarz tygodniowy, podsumowanie kalorii i makroskładników (B/W/T), posiłki według pory dnia, odhaczanie zjedzonych, licznik wody z dziennym celem.
- **Przepisy** — wyszukiwarka, kategorie, szczegóły ze składnikami i krokami, dodawanie/edycja, dodawanie do dziennika lub planowanie w kalendarzu.
- **Planer** — widok tygodnia, planowanie przepisów na konkretne dni i pory, kopiowanie dnia lub całego tygodnia do dziennika.
- **Notatnik** — listy zakupów, pomiary, samopoczucie.
- **Aktualizacje** — z GitHub Releases, z powiadomieniem o nowej wersji (sprawdzanie w tle co 12 h).

## Technologie

Kotlin · Jetpack Compose · Material 3 · Room · Navigation Compose · WorkManager · MVVM + Clean Architecture.

## Budowanie

Wymagane Android Studio (zawiera JDK). W folderze projektu:

```powershell
.\gradlew.bat assembleDebug
```

## Wydawanie nowej wersji

Wersje release są podpisywane kluczem z pliku `keystore/moja-dieta-release.jks` (dane w `keystore.properties`). **Te pliki nie trafiają do repozytorium** — trzymaj ich kopię zapasową. Bez tego samego klucza telefony nie zainstalują aktualizacji.

```powershell
.\wydaj-wersje.ps1 -Wersja 1.2.0 -Opis "Co się zmieniło w tej wersji"
```

Skrypt podbija numer wersji, buduje podpisany APK, wysyła zmiany na GitHub i tworzy wydanie.
