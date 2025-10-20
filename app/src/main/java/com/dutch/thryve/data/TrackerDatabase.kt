package com.dutch.thryve.data

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [MealData::class], version = 1, exportSchema = false)
abstract class TrackerDatabase : RoomDatabase(){
    abstract fun trackerDao() : TrackerDao
}