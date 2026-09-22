package com.example.data.macro

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * MacroDao: Interfaz DAO para operaciones de persistencia de macros y automatizaciones locales.
 * 
 * Todas las consultas de lectura exponen Flow reactivo y las operaciones de escritura
 * son funciones suspendidas para respetar la política de no bloqueo del hilo principal.
 */
@Dao
interface MacroDao {

    @Query("SELECT * FROM macros ORDER BY isPredefined DESC, name ASC")
    fun getAllMacros(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE category = :category ORDER BY name ASC")
    fun getMacrosByCategory(category: String): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE triggerKeyword = :trigger LIMIT 1")
    suspend fun getMacroByTrigger(trigger: String): MacroEntity?

    @Query("SELECT COUNT(*) FROM macros")
    suspend fun countMacros(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: MacroEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(macros: List<MacroEntity>)

    @Update
    suspend fun updateMacro(macro: MacroEntity)

    @Delete
    suspend fun deleteMacro(macro: MacroEntity)
}
