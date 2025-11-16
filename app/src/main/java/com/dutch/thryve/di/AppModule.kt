package com.dutch.thryve.di

import android.content.Context
import androidx.room.Room
import com.dutch.thryve.data.dao.PRDao
import com.dutch.thryve.data.dao.TrackerDao
import com.dutch.thryve.db.TrackerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(
        context, TrackerDatabase::class.java, "fsdfsd"
    ).build()


    @Provides
    @Singleton
    fun provideTrackerDao(database: TrackerDatabase): TrackerDao {
        return database.trackerDao()
    }

    @Provides
    @Singleton
    fun providePRDao(database: TrackerDatabase) : PRDao{
        return database.prDao()
    }
}