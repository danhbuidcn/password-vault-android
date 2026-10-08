package com.pwvault.app.di

import com.pwvault.app.export.CsvExporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExportModule {
    @Provides
    @Singleton
    fun provideCsvExporter(): CsvExporter = CsvExporter()
}
