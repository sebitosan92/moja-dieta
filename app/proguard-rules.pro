# Reguły ProGuard / R8 dla aplikacji Moja Dieta

# Encje Room i modele domenowe
-keep class com.example.mojadieta.data.local.entity.** { *; }
-keep class com.example.mojadieta.domain.model.** { *; }

# WorkManager, aktualizacje i widżety
-keep class com.example.mojadieta.update.** { *; }
-keep class com.example.mojadieta.widget.** { *; }

# Ostrzeżenia bibliotek zewnętrznych
-dontwarn androidx.room.paging.**
-dontwarn coil3.**
-dontwarn com.google.mlkit.**
-dontwarn java.lang.ClassValue

# Zachowanie adnotacji i sygnatur generycznych
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
