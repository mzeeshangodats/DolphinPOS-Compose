plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.retail.dolphinpos.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    // Firebase BOM must come first - Commented out until google-services.json is added
    // platform(libs.firebaseBom)
    // implementation("com.google.firebase:firebase-analytics")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.hilt)
    implementation(libs.refrofit)
    implementation(libs.refrofit.gson)
    implementation(libs.square.logging)
    implementation(libs.room.runtime)
    implementation(libs.room.pagging)
    ksp(libs.room.compiler)
    kapt(libs.hilt.compiler)
    implementation(libs.camera)
    implementation(libs.camera.core)
    implementation(libs.camera.view)
    implementation(libs.camera.extension)
    implementation(libs.camera.lifecycle)
    implementation(libs.mlkit.barcode)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(":common"))
    implementation(project(":domain"))

    // PAX Libraries
    implementation(files("../app/libs/GLComm_V1.12.01_20230515.jar"))
    implementation(files("../app/libs/jsch-0.1.54.jar"))
    implementation(files("../app/libs/PaxLog_1.0.11_20220921.jar"))
    implementation(files("../app/libs/POSLink_Core_Android_V2.00.07_20240912.jar"))
    implementation(files("../app/libs/POSLink_Admin_Android_Plugin_V2.01.00_20240913.jar"))
    implementation(files("../app/libs/POSLink_Semi_Android_Plugin_V2.01.00_20240913.jar"))
    implementation(files("../app/libs/BrotherPrintLibrary.aar"))

}