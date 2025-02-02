package net.matsudamper.gptclient.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.matsudamper.gptclient.room.entity.Project

@Dao
interface ProjectDao {
    @Query("SELECT * FROM project")
    fun getAll(): Flow<List<Project>>

    @Query("SELECT * FROM project where id = :projectId")
    fun get(projectId: Long): Flow<Project>

    @Insert
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)
}
