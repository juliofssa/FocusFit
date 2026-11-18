package com.focusfit.app.di

import android.content.Context
import androidx.room.Room
import com.focusfit.app.data.local.AppDatabase
import com.focusfit.app.data.local.BackupDao
import com.focusfit.app.data.local.DailyDao
import com.focusfit.app.data.local.PlanDao
import com.focusfit.app.data.local.ReadingDao
import com.focusfit.app.data.local.SuggestionDao
import com.focusfit.app.data.local.WeightDao
import com.focusfit.app.data.local.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "focusfit.db").build()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideDailyDao(db: AppDatabase): DailyDao = db.dailyDao()

    @Provides
    fun providePlanDao(db: AppDatabase): PlanDao = db.planDao()

    @Provides
    fun provideReadingDao(db: AppDatabase): ReadingDao = db.readingDao()

    @Provides
    fun provideSuggestionDao(db: AppDatabase): SuggestionDao = db.suggestionDao()

    @Provides
    fun provideWeightDao(db: AppDatabase): WeightDao = db.weightDao()

    @Provides
    fun provideBackupDao(db: AppDatabase): BackupDao = db.backupDao()
}
