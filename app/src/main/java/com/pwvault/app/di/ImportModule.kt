package com.pwvault.app.di

import com.pwvault.app.importer.CsvImportParser
import com.pwvault.app.importer.ImportDuplicateDetector
import com.pwvault.app.importer.XlsxImportParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImportModule {
    @Provides
    @Singleton
    fun provideCsvImportParser(): CsvImportParser = CsvImportParser()

    @Provides
    @Singleton
    fun provideXlsxImportParser(): XlsxImportParser = XlsxImportParser()

    @Provides
    @Singleton
    fun provideImportDuplicateDetector(): ImportDuplicateDetector = ImportDuplicateDetector()
}
