package com.dutch.thryve.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dutch.thryve.data.dao.PRDao
import com.dutch.thryve.domain.model.MealData
import com.dutch.thryve.data.dao.TrackerDao

@Database(entities = [MealData::class], version = 1, exportSchema = false)
abstract class TrackerDatabase : RoomDatabase(){
    abstract fun trackerDao() : TrackerDao
    abstract fun prDao() : PRDao
}