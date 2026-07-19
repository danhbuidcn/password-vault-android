package com.pwvault.app.di

import com.pwvault.app.data.TagRepository
import com.pwvault.app.data.VaultFileManager
import com.pwvault.app.data.VaultItemRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideVaultItemRepository(vaultFileManager: VaultFileManager): VaultItemRepository =
        VaultItemRepository(vaultFileManager)

    @Provides
    @Singleton
    fun provideTagRepository(vaultFileManager: VaultFileManager): TagRepository = TagRepository(vaultFileManager)
}
