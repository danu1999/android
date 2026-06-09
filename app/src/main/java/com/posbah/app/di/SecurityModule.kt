package com.posbah.app.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.posbah.app.security.IntegrityChecker
import com.posbah.app.security.SecurePreferences
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
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager =
        CredentialManager.create(context)

    @Provides
    @Singleton
    fun provideSecurePreferences(@ApplicationContext context: Context): SecurePreferences =
        SecurePreferences(context)

    @Provides
    @Singleton
    fun provideIntegrityChecker(@ApplicationContext context: Context): IntegrityChecker =
        IntegrityChecker(context)
}
