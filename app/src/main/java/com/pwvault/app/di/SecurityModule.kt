package com.pwvault.app.di

import android.content.Context
import com.pwvault.app.data.VaultFileManager
import com.pwvault.app.security.AutoLockPreferences
import com.pwvault.app.security.BiometricCredentialStore
import com.pwvault.app.security.BiometricKeystoreKeyProvider
import com.pwvault.app.security.BiometricUnlockManager
import com.pwvault.app.security.ClipboardClearer
import com.pwvault.app.security.KeyDerivation
import com.pwvault.app.security.LanguagePreferences
import com.pwvault.app.security.LockoutPolicy
import com.pwvault.app.security.LockoutStore
import com.pwvault.app.security.PinCredentialStore
import com.pwvault.app.security.PinKeystoreKeyProvider
import com.pwvault.app.security.PinManager
import com.pwvault.app.security.ThemePreferences
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

    @Provides
    @Singleton
    fun provideBiometricKeystoreKeyProvider(): BiometricKeystoreKeyProvider = BiometricKeystoreKeyProvider()

    @Provides
    @Singleton
    fun provideBiometricCredentialStore(
        @ApplicationContext context: Context,
    ): BiometricCredentialStore = BiometricCredentialStore(context)

    @Provides
    @Singleton
    fun provideBiometricUnlockManager(
        keystoreKeyProvider: BiometricKeystoreKeyProvider,
        credentialStore: BiometricCredentialStore,
    ): BiometricUnlockManager = BiometricUnlockManager(keystoreKeyProvider, credentialStore)

    @Provides
    @Singleton
    fun provideAutoLockPreferences(
        @ApplicationContext context: Context,
    ): AutoLockPreferences = AutoLockPreferences(context)

    @Provides
    @Singleton
    fun provideLockoutStore(
        @ApplicationContext context: Context,
    ): LockoutStore = LockoutStore(context)

    @Provides
    @Singleton
    fun provideLockoutPolicy(store: LockoutStore): LockoutPolicy = LockoutPolicy(store)

    @Provides
    @Singleton
    fun provideClipboardClearer(
        @ApplicationContext context: Context,
    ): ClipboardClearer = ClipboardClearer(context)

    @Provides
    @Singleton
    fun provideThemePreferences(
        @ApplicationContext context: Context,
    ): ThemePreferences = ThemePreferences(context)

    @Provides
    @Singleton
    fun provideLanguagePreferences(): LanguagePreferences = LanguagePreferences()
}
