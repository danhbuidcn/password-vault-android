package com.pwvault.app.di

import android.content.Context
import com.pwvault.app.data.VaultFileManager
import com.pwvault.app.security.KeyDerivation
import com.pwvault.app.security.PinCredentialStore
import com.pwvault.app.security.PinKeystoreKeyProvider
import com.pwvault.app.security.PinManager
import com.pwvault.app.security.VaultMetadataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    @Provides
    @Singleton
    fun provideKeyDerivation(): KeyDerivation = KeyDerivation()

    @Provides
    @Singleton
    fun provideVaultMetadataStore(
        @ApplicationContext context: Context,
    ): VaultMetadataStore = VaultMetadataStore(context)

    @Provides
    @Singleton
    fun provideVaultFileManager(
        @ApplicationContext context: Context,
    ): VaultFileManager = VaultFileManager(context)

    @Provides
    @Singleton
    fun providePinKeystoreKeyProvider(): PinKeystoreKeyProvider = PinKeystoreKeyProvider()

    @Provides
    @Singleton
    fun providePinCredentialStore(
        @ApplicationContext context: Context,
    ): PinCredentialStore = PinCredentialStore(context)

    @Provides
    @Singleton
    fun providePinManager(
        keyDerivation: KeyDerivation,
        keystoreKeyProvider: PinKeystoreKeyProvider,
        credentialStore: PinCredentialStore,
    ): PinManager = PinManager(keyDerivation, keystoreKeyProvider, credentialStore)
}
