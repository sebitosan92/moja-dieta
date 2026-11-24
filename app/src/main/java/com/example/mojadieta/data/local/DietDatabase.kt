package com.example.mojadieta.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mojadieta.data.local.dao.CustomProductDao
import com.example.mojadieta.data.local.dao.MealEntryDao
import com.example.mojadieta.data.local.dao.NoteDao
import com.example.mojadieta.data.local.dao.PlannedMealDao
import com.example.mojadieta.data.local.dao.RecipeDao
import com.example.mojadieta.data.local.dao.WaterDao
import com.example.mojadieta.data.local.dao.WeightDao
import com.example.mojadieta.data.local.dao.ActivityDao
import com.example.mojadieta.data.local.dao.BodyMetricDao
import com.example.mojadieta.data.local.dao.ProductUsageDao
import com.example.mojadieta.data.local.dao.ShoppingDao
import com.example.mojadieta.data.local.entity.ActivityEntity
import com.example.mojadieta.data.local.entity.ActivityTemplateEntity
import com.example.mojadieta.data.local.entity.BodyMetricEntity
import com.example.mojadieta.data.local.entity.DailyStepsEntity
import com.example.mojadieta.data.local.entity.PlannedActivityEntity
import com.example.mojadieta.data.local.entity.ProductUsageEntity
import com.example.mojadieta.data.local.entity.ShoppingItemEntity
import com.example.mojadieta.data.local.entity.CustomProductEntity
import com.example.mojadieta.data.local.entity.MealEntryEntity
import com.example.mojadieta.data.local.entity.NoteEntity
import com.example.mojadieta.data.local.entity.PlannedMealEntity
import com.example.mojadieta.data.local.entity.RecipeEntity
import com.example.mojadieta.data.local.entity.RecipeIngredientEntity
import com.example.mojadieta.data.local.entity.WaterEntity
import com.example.mojadieta.data.local.entity.WeightEntryEntity

@Database(
    entities = [
        RecipeEntity::class,
        MealEntryEntity::class,
        PlannedMealEntity::class,
        WaterEntity::class,
        NoteEntity::class,
        RecipeIngredientEntity::class,
        CustomProductEntity::class,
        WeightEntryEntity::class,
        ActivityEntity::class,
        ActivityTemplateEntity::class,
        PlannedActivityEntity::class,
        ShoppingItemEntity::class,
        BodyMetricEntity::class,
        DailyStepsEntity::class,
        ProductUsageEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class DietDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun plannedMealDao(): PlannedMealDao
    abstract fun waterDao(): WaterDao
    abstract fun noteDao(): NoteDao
    abstract fun customProductDao(): CustomProductDao
    abstract fun weightDao(): WeightDao
    abstract fun activityDao(): ActivityDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun productUsageDao(): ProductUsageDao

    companion object {
        private const val DATABASE_NAME = "moja_dieta.db"

        /**
         * Wersja 1 → 2: porcje i automatyczne liczenie w przepisach, składniki z gramaturą,
         * własne produkty. Istniejące dane użytkownika zostają nienaruszone.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN servings INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE recipes ADD COLUMN autoNutrition INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recipe_ingredients (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipeId INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        grams REAL NOT NULL,
                        kcal100 REAL NOT NULL,
                        protein100 REAL NOT NULL,
                        carbs100 REAL NOT NULL,
                        fat100 REAL NOT NULL,
                        FOREIGN KEY(recipeId) REFERENCES recipes(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_recipe_ingredients_recipeId ON recipe_ingredients (recipeId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_products (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        kcal REAL NOT NULL,
                        protein REAL NOT NULL,
                        carbs REAL NOT NULL,
                        fat REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Wersja 2 → 3: porcje i zdjęcia we wpisach dziennika, zdjęcia przepisów,
         * kody kreskowe własnych produktów, tabela pomiarów wagi.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_entries ADD COLUMN servings REAL NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE meal_entries ADD COLUMN imagePath TEXT")
                db.execSQL("ALTER TABLE recipes ADD COLUMN imagePath TEXT")
                db.execSQL("ALTER TABLE custom_products ADD COLUMN barcode TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weight_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        timeMillis INTEGER NOT NULL,
                        weightKg REAL NOT NULL,
                        source TEXT NOT NULL,
                        externalId TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_weight_entries_externalId ON weight_entries (externalId)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weight_entries_date ON weight_entries (date)")
            }
        }

        /**
         * Wersja 3 → 4: ulubione i tagi przepisów, błonnik/cukry/sól, aktywność, lista zakupów,
         * skład ciała i obwody, kroki z Health Connect, statystyki produktów.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE recipes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                for (table in listOf("recipes", "meal_entries", "custom_products")) {
                    for (col in listOf("fiber", "sugar", "salt")) {
                        db.execSQL("ALTER TABLE $table ADD COLUMN $col REAL")
                    }
                }
                for (col in listOf("fiber100", "sugar100", "salt100")) {
                    db.execSQL("ALTER TABLE recipe_ingredients ADD COLUMN $col REAL")
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS activities (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        startMillis INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT,
                        durationMin INTEGER NOT NULL,
                        intensity TEXT NOT NULL,
                        distanceKm REAL,
                        avgHeartRate INTEGER,
                        watts INTEGER,
                        calories INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        source TEXT NOT NULL,
                        externalId TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_activities_externalId ON activities (externalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activities_date ON activities (date)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS activity_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        durationMin INTEGER NOT NULL,
                        intensity TEXT NOT NULL,
                        distanceKm REAL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS planned_activities (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        durationMin INTEGER NOT NULL,
                        intensity TEXT NOT NULL,
                        note TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_planned_activities_date ON planned_activities (date)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        quantity TEXT NOT NULL,
                        groupName TEXT NOT NULL,
                        checked INTEGER NOT NULL,
                        manual INTEGER NOT NULL,
                        position INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS body_metrics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        timeMillis INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        value REAL NOT NULL,
                        source TEXT NOT NULL,
                        externalId TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_body_metrics_externalId ON body_metrics (externalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_body_metrics_date ON body_metrics (date)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_steps (
                        date INTEGER PRIMARY KEY NOT NULL,
                        steps INTEGER NOT NULL,
                        activeCalories INTEGER NOT NULL,
                        distanceKm REAL NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS product_usage (
                        `key` TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        count INTEGER NOT NULL,
                        lastUsed INTEGER NOT NULL,
                        favorite INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        const val NAME = DATABASE_NAME

        /** Zamyka bazę (przed przywróceniem kopii zapasowej). */
        fun closeInstance() = synchronized(this) {
            instance?.close()
            instance = null
        }

        @Volatile
        private var instance: DietDatabase? = null

        fun getInstance(context: Context): DietDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DietDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(PrepopulateCallback())
                    .build()
                    .also { instance = it }
            }
    }
}
