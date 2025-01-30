package net.matsudamper.gptclient.room.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.matsudamper.gptclient.room.entity.Project

@Dao
interface ProjectDao {
    @Query("SELECT * FROM project")
    fun getAll(): Flow<List<Project>>
}
