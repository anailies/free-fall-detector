package com.ana.falldetector.model

import androidx.lifecycle.LiveData
import com.ana.falldetector.database.DropDao
import com.ana.falldetector.database.DropDatabase
import com.ana.falldetector.init.FallDetector

object DropRepository {

    private var mDropDao: DropDao? = null

    init {
        if (FallDetector.applicationContext != null) {
            val database: DropDatabase = DropDatabase.getInstance(FallDetector.applicationContext!!)!!
            mDropDao = database.dropDao()
        }
    }

    suspend fun recordDrop(drop: Drop) {
        mDropDao?.insert(drop)
    }

    fun getAllDrops(): LiveData<List<Drop>>? {
        return mDropDao?.getAllDrops()
    }
}
