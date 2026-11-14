package com.example.mojadieta.data.repository

import com.example.mojadieta.data.local.dao.MealEntryDao
import com.example.mojadieta.data.local.dao.NoteDao
import com.example.mojadieta.data.local.dao.PlannedMealDao
import com.example.mojadieta.data.local.dao.RecipeDao
import com.example.mojadieta.data.local.dao.WaterDao
import com.example.mojadieta.data.local.entity.PlannedMealEntity
import com.example.mojadieta.data.mapper.toDomain
import com.example.mojadieta.data.mapper.toDomainOrNull
import com.example.mojadieta.data.mapper.toEntity
import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.Note
import com.example.mojadieta.domain.model.PlannedMeal
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.WaterIntake
import com.example.mojadieta.domain.repository.DiaryRepository
import com.example.mojadieta.domain.repository.NoteRepository
import com.example.mojadieta.domain.repository.PlannerRepository
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RecipeRepositoryImpl(private val dao: RecipeDao) : RecipeRepository {
    override fun observeRecipes(): Flow<List<Recipe>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeRecipe(id: Long): Flow<Recipe?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getRecipe(id: Long): Recipe? = dao.getById(id)?.toDomain()

    override suspend fun saveRecipe(recipe: Recipe): Long =
        if (recipe.id == 0L) {
            dao.insert(recipe.toEntity())
        } else {
            dao.update(recipe.toEntity())
            recipe.id
        }

    override suspend fun deleteRecipe(id: Long) = dao.deleteById(id)
}

class DiaryRepositoryImpl(private val dao: MealEntryDao) : DiaryRepository {
    override fun observeMeals(date: LocalDate): Flow<List<MealEntry>> =
        dao.observeByDate(date.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override suspend fun getMeals(date: LocalDate): List<MealEntry> =
        dao.getByDate(date.toEpochDay()).map { it.toDomain() }

    override suspend fun addMeal(entry: MealEntry): Long = dao.insert(entry.copy(id = 0).toEntity())

    override suspend fun addMeals(entries: List<MealEntry>) =
        dao.insertAll(entries.map { it.copy(id = 0).toEntity() })

    override suspend fun setEaten(id: Long, eaten: Boolean) = dao.setEaten(id, eaten)

    override suspend fun deleteMeal(id: Long) = dao.deleteById(id)
}

class PlannerRepositoryImpl(private val dao: PlannedMealDao) : PlannerRepository {
    override fun observePlannedMeals(start: LocalDate, end: LocalDate): Flow<List<PlannedMeal>> =
        dao.observeBetween(start.toEpochDay(), end.toEpochDay())
            .map { list -> list.mapNotNull { it.toDomainOrNull() } }

    override suspend fun getPlannedMeals(start: LocalDate, end: LocalDate): List<PlannedMeal> =
        dao.getBetween(start.toEpochDay(), end.toEpochDay()).mapNotNull { it.toDomainOrNull() }

    override suspend fun addPlannedMeal(date: LocalDate, mealType: MealType, recipeId: Long): Long =
        dao.insert(
            PlannedMealEntity(
                date = date.toEpochDay(),
                mealType = mealType.name,
                recipeId = recipeId
            )
        )

    override suspend fun deletePlannedMeal(id: Long) = dao.deleteById(id)
}

class WaterRepositoryImpl(private val dao: WaterDao) : WaterRepository {
    override fun observeWater(date: LocalDate): Flow<WaterIntake> {
        val day = date.toEpochDay()
        return combine(dao.observe(day), dao.observeLatestGoal(day)) { entity, latestGoal ->
            WaterIntake(
                date = date,
                amountMl = entity?.amountMl ?: 0,
                goalMl = entity?.goalMl ?: latestGoal ?: WaterIntake.DEFAULT_GOAL_ML
            )
        }
    }

    override suspend fun changeAmount(date: LocalDate, deltaMl: Int) =
        dao.changeAmount(date.toEpochDay(), deltaMl, WaterIntake.DEFAULT_GOAL_ML)

    override suspend fun setGoal(date: LocalDate, goalMl: Int) =
        dao.setGoal(date.toEpochDay(), goalMl)
}

class NoteRepositoryImpl(private val dao: NoteDao) : NoteRepository {
    override fun observeNotes(): Flow<List<Note>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getNote(id: Long): Note? = dao.getById(id)?.toDomain()

    override suspend fun saveNote(note: Note): Long =
        if (note.id == 0L) {
            dao.insert(note.toEntity())
        } else {
            dao.update(note.toEntity())
            note.id
        }

    override suspend fun deleteNote(id: Long) = dao.deleteById(id)
}
