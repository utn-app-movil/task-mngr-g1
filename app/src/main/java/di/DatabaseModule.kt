package di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import data.local.AppDatabase
import data.local.ITaskDao
import javax.inject.Singleton

@Module
@InstallIn
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
            @ApplicationContext context: Context
        ): AppDatabase = Room.databaseBuilder(context,
        AppDatabase::class.java, "task_database").build()

    @Provides
    fun provideTaskDao(database: AppDatabase): ITaskDao = database.taskDao()
}