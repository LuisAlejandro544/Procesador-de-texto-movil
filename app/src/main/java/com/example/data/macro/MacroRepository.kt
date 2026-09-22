package com.example.data.macro

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * MacroRepository: Repositorio para la gestión de macros locales.
 * 
 * Centraliza las llamadas a base de datos protegiendo el hilo principal (Dispatchers.IO).
 */
class MacroRepository(private val macroDao: MacroDao) {

    val allMacros: Flow<List<MacroEntity>> = macroDao.getAllMacros()
        .flowOn(Dispatchers.IO)

    fun getMacrosByCategory(category: String): Flow<List<MacroEntity>> {
        return macroDao.getMacrosByCategory(category).flowOn(Dispatchers.IO)
    }

    suspend fun getMacroByTrigger(trigger: String): MacroEntity? = withContext(Dispatchers.IO) {
        macroDao.getMacroByTrigger(trigger)
    }

    suspend fun insertMacro(macro: MacroEntity): Long = withContext(Dispatchers.IO) {
        macroDao.insertMacro(macro)
    }

    suspend fun updateMacro(macro: MacroEntity) = withContext(Dispatchers.IO) {
        macroDao.updateMacro(macro)
    }

    suspend fun deleteMacro(macro: MacroEntity) = withContext(Dispatchers.IO) {
        macroDao.deleteMacro(macro)
    }

    suspend fun countMacros(): Int = withContext(Dispatchers.IO) {
        macroDao.countMacros()
    }
}
