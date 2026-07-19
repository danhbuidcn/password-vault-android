package com.pwvault.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordTemplateDao {
    @Query("SELECT * FROM password_templates ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PasswordTemplateEntity>>

    @Insert
    suspend fun insert(template: PasswordTemplateEntity): Long

    @Delete
    suspend fun delete(template: PasswordTemplateEntity)
}
