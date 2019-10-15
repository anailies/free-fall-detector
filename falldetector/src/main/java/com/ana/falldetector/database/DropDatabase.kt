package com.ana.falldetector.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ana.falldetector.model.Drop

@Database(entities = [Drop::class], version = 2)
abstract class DropDatabase : RoomDatabase() {
    abstract fun dropDao(): DropDao

    companion object {
        private var INSTANCE: DropDatabase? = null

        fun getInstance(context: Context): DropDatabase? {
            if (INSTANCE == null) {
                synchronized(DropDatabase::class.java) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext, DropDatabase::class.java, "drop")
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE
        }
    }
}