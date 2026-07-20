package com.pwvault.app.di

import android.content.Context
import com.pwvault.app.data.AutoBackupWriter
import com.pwvault.app.data.PasswordTemplateRepository
import com.pwvault.app.data.TagRepository
import com.pwvault.app.data.VaultFileManager
import com.pwvault.app.data.VaultItemRepository
import com.pwvault.app.security.BackupPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideBackupPreferences(
        @ApplicationContext context: Context,
    ): BackupPreferences = BackupPreferences(context)

    @Provides
    @Singleton
    fun provideAutoBackupWriter(
        @ApplicationContext context: Context,
        vaultFileManager: VaultFileManager,
        backupPreferences: BackupPreferences,
    ): AutoBackupWriter = AutoBackupWriter(context, vaultFileManager, backupPreferences)

    @Provides
    @Singleton
    fun provideVaultItemRepository(
        vaultFileManager: VaultFileManager,
        autoBackupWriter: AutoBackupWriter,
    ): VaultItemRepository = VaultItemRepository(vaultFileManager, autoBackupWriter)

    @Provides
    @Singleton
    fun provideTagRepository(vaultFileManager: VaultFileManager): TagRepository = TagRepository(vaultFileManager)

    @Provides
    @Singleton
    fun providePasswordTemplateRepository(vaultFileManager: VaultFileManager): PasswordTemplateRepository =
        PasswordTemplateRepository(vaultFileManager)
}
