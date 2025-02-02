package net.matsudamper.gptclient.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity("project")
@TypeConverters(ProjectId.Converter::class)
data class Project(
    @ColumnInfo("id") @PrimaryKey(autoGenerate = true) val id: ProjectId = ProjectId(0),
    @ColumnInfo("index") val index: Int,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("model_name") val modelName: String,
    @ColumnInfo("system_message") val systemMessage: String,
)
