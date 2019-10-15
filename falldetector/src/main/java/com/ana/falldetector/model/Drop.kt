package com.ana.falldetector.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "drop")
data class Drop (var duration : Double,
                 @PrimaryKey (autoGenerate = false)
                 var timestamp: Long)