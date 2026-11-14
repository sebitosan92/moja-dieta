# Reguły ProGuard dla aplikacji Moja Dieta.
# Minifikacja jest wyłączona w buildzie release; plik pozostaje na potrzeby przyszłej konfiguracji.
-keep class com.example.mojadieta.data.local.entity.** { *; }
