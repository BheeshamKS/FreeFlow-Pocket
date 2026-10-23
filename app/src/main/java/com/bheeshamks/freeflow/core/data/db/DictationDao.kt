package com.bheeshamks.freeflow.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local dictation history and metrics.
 */
@Dao
interface DictationDao {

    @Query("SELECT * FROM dictation_history ORDER BY timestamp DESC")
    fun getAllDictations(): Flow<List<DictationEntity>>

    @Query("SELECT * FROM dictation_history WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchDictations(query: String): Flow<List<DictationEntity>>

    @Query("SELECT SUM(wordCount) FROM dictation_history")
    fun getTotalWordCount(): Flow<Int?>

    @Query("SELECT AVG(latencyMs) FROM dictation_history WHERE latencyMs > 0")
    fun getAverageLatency(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM dictation_history")
    fun getTotalDictationCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictation(entity: DictationEntity): Long

    @Delete
    suspend fun deleteDictation(entity: DictationEntity)

    @Query("DELETE FROM dictation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM dictation_history")
    suspend fun clearAll()
}
