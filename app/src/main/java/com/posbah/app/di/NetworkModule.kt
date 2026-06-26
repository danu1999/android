package com.posbah.app.di

import com.posbah.app.data.remote.api.BmpApiService
import com.posbah.app.data.remote.api.MigrationApiService
import com.posbah.app.data.remote.api.PosApiService
import com.posbah.app.security.SecurePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// NetworkModule — menggantikan DatabaseModule.kt
// Menyediakan Retrofit instance + semua ApiService ke Hilt DI graph.
// Token diambil dari SecurePreferences setiap request (lazy evaluation),
// sehingga token terbaru selalu terpakai setelah login/refresh.
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://zedmz.cloud/"

    @Provides
    @Singleton
    fun provideAuthInterceptor(securePrefs: SecurePreferences): Interceptor {
        return Interceptor { chain ->
            val token = securePrefs.currentGoogleSub ?: ""
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-client-version", com.posbah.app.BuildConfig.VERSION_NAME)
                .build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePosApiService(retrofit: Retrofit): PosApiService {
        return retrofit.create(PosApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBmpApiService(retrofit: Retrofit): BmpApiService {
        return retrofit.create(BmpApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMigrationApiService(retrofit: Retrofit): MigrationApiService {
        return retrofit.create(MigrationApiService::class.java)
    }
}
