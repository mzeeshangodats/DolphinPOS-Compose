plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt)
    alias(libs.plugins.safe.args.gradle.plugin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.app.distribution)
}

android {
    namespace = "com.retail.dolphinpos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.dolphinpos"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunner = "dagger.hilt.android.testing.HiltTestRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")

//            firebaseAppDistribution {
//                releaseNotesFile = "release-notes.txt"
//                groups = "dms-internal"
//            }
        }
        release {
            isMinifyEnabled = false
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        flavorDimensions += "default"
        buildConfig = true
    }

    kapt {
        correctErrorTypes = true
    }

    productFlavors {
        create("dev") {
            applicationId = "com.dolphin.retail.pos"
            dimension = "default"
            resValue("string", "app_name", "Dolphin POS (Dev)")
            buildConfigField("String", "BUILD_TYPE", "\"DEV\"")
            buildConfigField("String", "BASE_URL", "\"https://www.dev-retail.gotmsolutions.com/api/\"")
            versionNameSuffix = "-dev"

        }
        create("uat") {
            applicationId = "com.dolphin.retail.pos"
            dimension = "default"
            resValue("string", "app_name", "Dolphin POS (Uat)")
            buildConfigField("String", "BUILD_TYPE", "\"UAT\"")
            buildConfigField("String", "BASE_URL", "\"https://uat-lingerie.gotmsolutions.com/api/\"")
            versionNameSuffix = "-uat"

        }
        create("prod") {
            applicationId = "com.dolphin.retail.pos"
            dimension = "default"
            resValue("string", "app_name", "Dolphin POS")
            buildConfigField("String", "BUILD_TYPE", "\"PROD\"")
            buildConfigField("String", "BASE_URL", "\"https://uat-lingerie.gotmsolutions.com/api/\"")
            versionNameSuffix = "-prod"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.hilt)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.fragment.testing)
    kapt(libs.hilt.compiler)

    implementation(libs.swipe.refresh)
    implementation(libs.refrofit)
    implementation(libs.refrofit.gson)
    implementation(libs.square.logging)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation(libs.room.runtime)
    implementation(libs.room.pagging)
    ksp(libs.room.compiler)

    implementation(libs.sdp)
    implementation(libs.coil)
    implementation(libs.coil.network)
    implementation(libs.security)
    implementation(libs.starPrinter)
    platform(libs.firebaseBom)
    implementation(libs.camera)
    implementation(libs.camera.core)
    implementation(libs.camera.view)
    implementation(libs.camera.extension)
    implementation(libs.camera.lifecycle)
    implementation(libs.mlkit.barcode)
    implementation(libs.workManager)
    implementation(libs.hilt.workManager)
    kapt(libs.hilt.compiler.workManager)
    implementation(libs.gson)
    implementation(libs.zxing)
    implementation(libs.timber)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.coil.compose)
    
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.mock.io)
    testImplementation(libs.kotlin.coroutine.test)
    testImplementation(libs.live.data.test)
    kaptAndroidTest(libs.hilt.testing)

    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":presentation"))

    implementation(files("libs/GLComm_V1.12.01_20230515.jar"))
    implementation(files("libs/jsch-0.1.54.jar"))
    implementation(files("libs/PaxLog_1.0.11_20220921.jar"))
    implementation(files("libs/POSLink_Core_Android_V2.00.07_20240912.jar"))
    implementation(files("libs/POSLink_Semi_Android_Plugin_V2.01.00_20240913.jar"))

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.navigation.testing)
    androidTestImplementation(libs.hilt.testing)
}