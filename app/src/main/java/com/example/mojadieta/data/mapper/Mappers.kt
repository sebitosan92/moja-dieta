package com.example.mojadieta.data.mapper

import com.example.mojadieta.data.local.entity.CustomProductEntity
import com.example.mojadieta.data.local.entity.MealEntryEntity
import com.example.mojadieta.data.local.entity.RecipeIngredientEntity
import com.example.mojadieta.data.local.entity.WeightEntryEntity
import com.example.mojadieta.data.local.entity.NoteEntity
import com.example.mojadieta.data.local.entity.PlannedMealWithRecipe
import com.example.mojadieta.data.local.entity.RecipeEntity
import com.example.mojadieta.domain.model.FoodProduct
import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.RecipeIngredient
import com.example.mojadieta.domain.model.WeightEntry
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.Note
import com.example.mojadieta.domain.model.PlannedMeal
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.RecipeCategory
import java.time.LocalDate

fun RecipeEntity.toDomain() = Recipe(
    id = id,
    name = name,
    category = RecipeCategory.fromName(category),
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    ingredients = ingredients,
    instructions = instructions,
    servings = servings,
    autoNutrition = autoNutrition,
    imagePath = imagePath
)

fun Recipe.toEntity() = RecipeEntity(
    id = id,
    name = name,
    category = category.name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    ingredients = ingredients,
    instructions = instructions,
    servings = servings,
    autoNutrition = autoNutrition,
    imagePath = imagePath
)

fun RecipeIngredientEntity.toDomain() = RecipeIngredient(
    id = id,
    name = name,
    grams = grams,
    kcal100 = kcal100,
    protein100 = protein100,
    carbs100 = carbs100,
    fat100 = fat100
)

fun RecipeIngredient.toEntity(recipeId: Long, position: Int) = RecipeIngredientEntity(
    id = id,
    recipeId = recipeId,
    position = position,
    name = name,
    grams = grams,
    kcal100 = kcal100,
    protein100 = protein100,
    carbs100 = carbs100,
    fat100 = fat100
)

fun CustomProductEntity.toDomain() = FoodProduct(
    name = name,
    kcal = kcal,
    protein = protein,
    carbs = carbs,
    fat = fat,
    customId = id,
    barcode = barcode
)

fun WeightEntryEntity.toDomain() = WeightEntry(
    id = id,
    date = LocalDate.ofEpochDay(date),
    timeMillis = timeMillis,
    weightKg = weightKg,
    source = source,
    externalId = externalId
)

fun WeightEntry.toEntity() = WeightEntryEntity(
    id = id,
    date = date.toEpochDay(),
    timeMillis = timeMillis,
    weightKg = weightKg,
    source = source,
    externalId = externalId
)

fun MealEntryEntity.toDomain() = MealEntry(
    id = id,
    date = LocalDate.ofEpochDay(date),
    mealType = MealType.fromName(mealType),
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    recipeId = recipeId,
    isEaten = eaten,
    servings = servings,
    imagePath = imagePath
)

fun MealEntry.toEntity() = MealEntryEntity(
    id = id,
    date = date.toEpochDay(),
    mealType = mealType.name,
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    recipeId = recipeId,
    eaten = isEaten,
    servings = servings,
    imagePath = imagePath
)

fun PlannedMealWithRecipe.toDomainOrNull(): PlannedMeal? {
    val recipeEntity = recipe ?: return null
    return PlannedMeal(
        id = planned.id,
        date = LocalDate.ofEpochDay(planned.date),
        mealType = MealType.fromName(planned.mealType),
        recipe = recipeEntity.toDomain()
    )
}

fun NoteEntity.toDomain() = Note(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Note.toEntity() = NoteEntity(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)
