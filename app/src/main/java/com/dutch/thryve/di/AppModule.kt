package com.dutch.thryve.di

import android.content.Context
import androidx.room.Room
import com.dutch.thryve.data.TrackerDao
import com.dutch.thryve.data.TrackerDatabase
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
    fun provideDao(database: TrackerDatabase): TrackerDao {
        return database.trackerDao()
    }
}