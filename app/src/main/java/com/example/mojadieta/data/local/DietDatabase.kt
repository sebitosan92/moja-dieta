package com.example.mojadieta.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mojadieta.data.local.dao.MealEntryDao
import com.example.mojadieta.data.local.dao.NoteDao
import com.example.mojadieta.data.local.dao.PlannedMealDao
import com.example.mojadieta.data.local.dao.RecipeDao
import com.example.mojadieta.data.local.dao.WaterDao
import com.example.mojadieta.data.local.entity.MealEntryEntity
import com.example.mojadieta.data.local.entity.NoteEntity
import com.example.mojadieta.data.local.entity.PlannedMealEntity
import com.example.mojadieta.data.local.entity.RecipeEntity
import com.example.mojadieta.data.local.entity.WaterEntity

@Database(
    entities = [
        RecipeEntity::class,
        MealEntryEntity::class,
        PlannedMealEntity::class,
        WaterEntity::class,
        NoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DietDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun plannedMealDao(): PlannedMealDao
    abstract fun waterDao(): WaterDao
    abstract fun noteDao(): NoteDao

    companion object {
        private const val DATABASE_NAME = "moja_dieta.db"

        @Volatile
        private var instance: DietDatabase? = null

        fun getInstance(context: Context): DietDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DietDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(PrepopulateCallback())
                    .build()
                    .also { instance = it }
            }
    }
}
