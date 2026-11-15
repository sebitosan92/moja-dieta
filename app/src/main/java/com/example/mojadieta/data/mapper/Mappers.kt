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
import com.example.mojadieta.data.local.entity.ActivityEntity
import com.example.mojadieta.data.local.entity.ActivityTemplateEntity
import com.example.mojadieta.data.local.entity.BodyMetricEntity
import com.example.mojadieta.data.local.entity.DayTotalsRow
import com.example.mojadieta.data.local.entity.PlannedActivityEntity
import com.example.mojadieta.data.local.entity.ShoppingItemEntity
import com.example.mojadieta.domain.model.ActivityEntry
import com.example.mojadieta.domain.model.ActivityTemplate
import com.example.mojadieta.domain.model.ActivityType
import com.example.mojadieta.domain.model.BodyMetric
import com.example.mojadieta.domain.model.BodyMetricType
import com.example.mojadieta.domain.model.DayTotals
import com.example.mojadieta.domain.model.Intensity
import com.example.mojadieta.domain.model.PlannedActivity
import com.example.mojadieta.domain.model.ShoppingItem
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
    imagePath = imagePath,
    favorite = favorite,
    tags = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    fiber = fiber,
    sugar = sugar,
    salt = salt
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
    imagePath = imagePath,
    favorite = favorite,
    tags = tags.map { it.trim().replace(",", " ") }.filter { it.isNotEmpty() }.distinct().joinToString(","),
    fiber = fiber,
    sugar = sugar,
    salt = salt
)

fun RecipeIngredientEntity.toDomain() = RecipeIngredient(
    id = id,
    name = name,
    grams = grams,
    kcal100 = kcal100,
    protein100 = protein100,
    carbs100 = carbs100,
    fat100 = fat100,
    fiber100 = fiber100,
    sugar100 = sugar100,
    salt100 = salt100
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
    fat100 = fat100,
    fiber100 = fiber100,
    sugar100 = sugar100,
    salt100 = salt100
)

fun CustomProductEntity.toDomain() = FoodProduct(
    name = name,
    kcal = kcal,
    protein = protein,
    carbs = carbs,
    fat = fat,
    customId = id,
    barcode = barcode,
    fiber = fiber,
    sugar = sugar,
    salt = salt
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
    imagePath = imagePath,
    fiber = fiber,
    sugar = sugar,
    salt = salt
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
    imagePath = imagePath,
    fiber = fiber,
    sugar = sugar,
    salt = salt
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

private fun intensityOf(name: String) = Intensity.entries.firstOrNull { it.name == name } ?: Intensity.MODERATE

fun ActivityEntity.toDomain() = ActivityEntry(
    id = id,
    date = LocalDate.ofEpochDay(date),
    startMillis = startMillis,
    type = ActivityType.fromName(type),
    title = title,
    durationMin = durationMin,
    intensity = intensityOf(intensity),
    distanceKm = distanceKm,
    avgHeartRate = avgHeartRate,
    watts = watts,
    calories = calories,
    note = note,
    source = source,
    externalId = externalId
)

fun ActivityEntry.toEntity() = ActivityEntity(
    id = id,
    date = date.toEpochDay(),
    startMillis = startMillis,
    type = type.name,
    title = title,
    durationMin = durationMin,
    intensity = intensity.name,
    distanceKm = distanceKm,
    avgHeartRate = avgHeartRate,
    watts = watts,
    calories = calories,
    note = note,
    source = source,
    externalId = externalId
)

fun ActivityTemplateEntity.toDomain() = ActivityTemplate(
    id = id,
    name = name,
    type = ActivityType.fromName(type),
    durationMin = durationMin,
    intensity = intensityOf(intensity),
    distanceKm = distanceKm
)

fun ActivityTemplate.toEntity() = ActivityTemplateEntity(
    id = id,
    name = name,
    type = type.name,
    durationMin = durationMin,
    intensity = intensity.name,
    distanceKm = distanceKm
)

fun PlannedActivityEntity.toDomain() = PlannedActivity(
    id = id,
    date = LocalDate.ofEpochDay(date),
    type = ActivityType.fromName(type),
    durationMin = durationMin,
    intensity = intensityOf(intensity),
    note = note
)

fun PlannedActivity.toEntity() = PlannedActivityEntity(
    id = id,
    date = date.toEpochDay(),
    type = type.name,
    durationMin = durationMin,
    intensity = intensity.name,
    note = note
)

fun ShoppingItemEntity.toDomain() = ShoppingItem(
    id = id,
    name = name,
    quantity = quantity,
    group = groupName,
    checked = checked,
    manual = manual,
    position = position
)

fun ShoppingItem.toEntity() = ShoppingItemEntity(
    id = id,
    name = name,
    quantity = quantity,
    groupName = group,
    checked = checked,
    manual = manual,
    position = position
)

fun BodyMetricEntity.toDomain() = BodyMetric(
    id = id,
    date = LocalDate.ofEpochDay(date),
    timeMillis = timeMillis,
    type = BodyMetricType.fromName(type),
    value = value,
    source = source,
    externalId = externalId
)

fun BodyMetric.toEntity() = BodyMetricEntity(
    id = id,
    date = date.toEpochDay(),
    timeMillis = timeMillis,
    type = type.name,
    value = value,
    source = source,
    externalId = externalId
)

fun DayTotalsRow.toDomain() = DayTotals(
    date = LocalDate.ofEpochDay(date),
    eatenCalories = eatenCalories,
    eatenProtein = eatenProtein,
    eatenCarbs = eatenCarbs,
    eatenFat = eatenFat,
    entries = entries,
    notEaten = notEaten
)
