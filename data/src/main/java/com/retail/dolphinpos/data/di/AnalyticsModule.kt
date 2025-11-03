package com.retail.dolphinpos.data.di

import android.content.Context
import android.os.Bundle
// import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Commented out until Firebase is properly configured with google-services.json
// @Module
// @InstallIn(SingletonComponent::class)
object AnalyticsModule {

    // @Singleton
    // @Provides
    // fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics {
    //     return FirebaseAnalytics.getInstance(context)
    // }

    interface AnalyticsTracker {
        fun logEvent(name: String, params: Bundle?)
        fun setUserProperty(name: String, value: String?)
        // Add other analytics methods you use
    }

    // @Singleton
    // @Provides
    // fun provideFirebaseAnalyticsTracker(firebaseAnalytics: FirebaseAnalytics): AnalyticsTracker {
    //     return object : AnalyticsTracker {
    //         override fun logEvent(name: String, params: Bundle?) {
    //             firebaseAnalytics.logEvent(name, params)
    //         }
    //         override fun setUserProperty(name: String, value: String?) {
    //             firebaseAnalytics.setUserProperty(name, value)
    //         }
    //     }
    // }
}