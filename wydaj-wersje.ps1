# Wydaje nową wersję aplikacji Moja Dieta na GitHubie.
# Użycie (w PowerShell, w folderze projektu):
#   .\wydaj-wersje.ps1 -Wersja 1.2.0 -Opis "Nowy wygląd planera, poprawki błędów"
# Skrypt: podbija numer wersji, buduje podpisany plik APK, zapisuje zmiany w git,
# wysyła je na GitHub i tworzy wydanie z plikiem APK. Aplikacje na telefonach
# same wykryją nową wersję (okno przy starcie + powiadomienie).
param(
    [Parameter(Mandatory = $true)][string]$Wersja,
    [string]$Opis = "Poprawki i ulepszenia."
)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if ($Wersja -notmatch '^\d+\.\d+\.\d+$') { throw "Podaj wersję w formacie 1.2.0" }
if (-not (Test-Path keystore.properties)) { throw "Brak pliku keystore.properties (klucz podpisywania)." }

$gh = (Get-Command gh -ErrorAction SilentlyContinue).Source
if (-not $gh) { $gh = 'C:\Program Files\GitHub CLI\gh.exe' }
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }

$props = @{}
Get-Content version.properties | ForEach-Object { if ($_ -match '^(\w+)=(.*)$') { $props[$matches[1]] = $matches[2].Trim() } }
$kod = [int]$props['versionCode'] + 1
Write-Host "Wydaję wersję $Wersja (versionCode $kod)..." -ForegroundColor Green
[System.IO.File]::WriteAllText("$PSScriptRoot\version.properties", "versionCode=$kod`nversionName=$Wersja`n")

& .\gradlew.bat assembleRelease
if ($LASTEXITCODE -ne 0) { throw "Budowanie nie powiodło się." }

$apk = "moja-dieta-$Wersja.apk"
Copy-Item app\build\outputs\apk\release\app-release.apk $apk -Force

git add -A
git commit -m "Wersja $Wersja"
git push
& $gh release create "v$Wersja" $apk --title "Moja Dieta $Wersja" --notes $Opis
if ($LASTEXITCODE -ne 0) { throw "Nie udało się utworzyć wydania na GitHubie." }
Remove-Item $apk
Write-Host "Gotowe! Wersja $Wersja jest na GitHubie." -ForegroundColor Green
