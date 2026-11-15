package com.example.mojadieta.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val ingredients: String,
    val instructions: String,
    // Kolumny dodane w wersji 2 bazy (migracja 1→2) – stąd wartości domyślne.
    @ColumnInfo(defaultValue = "1") val servings: Int = 1,
    @ColumnInfo(defaultValue = "0") val autoNutrition: Boolean = false,
    // Wersja 3 bazy:
    val imagePath: String? = null,
    // Wersja 4 bazy:
    @ColumnInfo(defaultValue = "0") val favorite: Boolean = false,
    /** Tagi rozdzielone przecinkami. */
    @ColumnInfo(defaultValue = "") val tags: String = "",
    val fiber: Double? = null,
    val sugar: Double? = null,
    val salt: Double? = null
)

/** Składnik przepisu z gramaturą i wartościami na 100 g (wersja 2 bazy). */
@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId")]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val position: Int,
    val name: String,
    val grams: Double,
    val kcal100: Double,
    val protein100: Double,
    val carbs100: Double,
    val fat100: Double,
    // Wersja 4 bazy:
    val fiber100: Double? = null,
    val sugar100: Double? = null,
    val salt100: Double? = null
)

/** Własny produkt użytkownika (wartości na 100 g). */
@Entity(tableName = "custom_products")
data class CustomProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    // Wersja 3 bazy: kod kreskowy zeskanowanego produktu
    val barcode: String? = null,
    // Wersja 4 bazy:
    val fiber: Double? = null,
    val sugar: Double? = null,
    val salt: Double? = null
)

/** Pomiar wagi (wersja 3 bazy). externalId = id rekordu z Health Connect (do pomijania duplikatów). */
@Entity(
    tableName = "weight_entries",
    indices = [Index(value = ["externalId"], unique = true), Index("date")]
)
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val timeMillis: Long,
    val weightKg: Double,
    val source: String,
    val externalId: String?
)

/** Wpis posiłku w dzienniku. Data przechowywana jako epochDay (LocalDate.toEpochDay()). */
@Entity(
    tableName = "meal_entries",
    indices = [Index("date")]
)
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val mealType: String,
    val name: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val recipeId: Long?,
    val eaten: Boolean,
    // Wersja 3 bazy:
    @ColumnInfo(defaultValue = "1") val servings: Double = 1.0,
    val imagePath: String? = null,
    // Wersja 4 bazy:
    val fiber: Double? = null,
    val sugar: Double? = null,
    val salt: Double? = null
)

@Entity(
    tableName = "planned_meals",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId"), Index("date")]
)
data class PlannedMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val mealType: String,
    val recipeId: Long
)

data class PlannedMealWithRecipe(
    @Embedded val planned: PlannedMealEntity,
    @Relation(parentColumn = "recipeId", entityColumn = "id")
    val recipe: RecipeEntity?
)

@Entity(tableName = "water_intake")
data class WaterEntity(
    @PrimaryKey val date: Long,
    val amountMl: Int,
    val goalMl: Int
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

// ---------------- Wersja 4 bazy ----------------

@Entity(
    tableName = "activities",
    indices = [Index(value = ["externalId"], unique = true), Index("date")]
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val startMillis: Long,
    val type: String,
    val title: String?,
    val durationMin: Int,
    val intensity: String,
    val distanceKm: Double?,
    val avgHeartRate: Int?,
    val watts: Int?,
    val calories: Int,
    val note: String,
    val source: String,
    val externalId: String?
)

@Entity(tableName = "activity_templates")
data class ActivityTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val durationMin: Int,
    val intensity: String,
    val distanceKm: Double?
)

@Entity(tableName = "planned_activities", indices = [Index("date")])
data class PlannedActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val type: String,
    val durationMin: Int,
    val intensity: String,
    val note: String
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String,
    val groupName: String,
    val checked: Boolean,
    val manual: Boolean,
    val position: Int
)

@Entity(
    tableName = "body_metrics",
    indices = [Index(value = ["externalId"], unique = true), Index("date")]
)
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val timeMillis: Long,
    val type: String,
    val value: Double,
    val source: String,
    val externalId: String?
)

/** Kroki i aktywne kalorie z Health Connect, jeden wiersz na dzień. */
@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey val date: Long,
    val steps: Int,
    val activeCalories: Int,
    val distanceKm: Double
)

/** Statystyka użycia produktu w składnikach i dzienniku. Klucz = nazwa po normalizacji. */
@Entity(tableName = "product_usage")
data class ProductUsageEntity(
    @PrimaryKey val key: String,
    val name: String,
    val count: Int,
    val lastUsed: Long,
    val favorite: Boolean
)

/** Suma dnia (wynik zapytania). */
data class DayTotalsRow(
    val date: Long,
    val eatenCalories: Int,
    val eatenProtein: Double,
    val eatenCarbs: Double,
    val eatenFat: Double,
    val entries: Int,
    val notEaten: Int
)
