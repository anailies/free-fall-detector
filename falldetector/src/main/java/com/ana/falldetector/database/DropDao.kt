package com.ana.falldetector.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ana.falldetector.model.Drop

@Dao
interface DropDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert (drop: Drop)

    @Query ("SELECT * FROM `drop`")
    fun getAllDrops() : LiveData<List<Drop>>
}