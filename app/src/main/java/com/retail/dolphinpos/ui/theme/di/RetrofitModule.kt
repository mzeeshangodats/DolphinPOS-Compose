package com.retail.dolphinpos.ui.theme.di

import com.retail.dolphinpos.BuildConfig
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.common.network.NoConnectivityException
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
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    @Provides
    @Named("AuthInterceptor")
    fun provideAuthInterceptor(
        preferenceManager: PreferenceManager
    ): Interceptor {
        return Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${preferenceManager.getAccessToken()}")
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(newRequest)
        }
    }

    @Provides
    @Named("ConnectivityInterceptor")
    fun provideConnectivityInterceptor(
        networkMonitor: NetworkMonitor,
    ): Interceptor =
        Interceptor { chain ->
            if (!networkMonitor.isNetworkAvailable()) {
                throw NoConnectivityException()
            }
            chain.proceed(chain.request())
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @Named("AuthInterceptor") authInterceptor: Interceptor,
        @Named("ConnectivityInterceptor") connectivityInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(connectivityInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .retryOnConnectionFailure(true)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

}