plugins {
    alias(libs.plugins.android.application)
    //id("com.android.application")
    //id("org.jetbrains.kotlin.android")
    //id("org.jetbrains.kotlin.plugin.compose")
    //id("com.google.devtools.ksp") // Necesario para procesar las anotaciones
    //id("com.google.dagger.hilt.android")
    //alias(libs.plugins.kotlin.android)
}

android {
    namespace = "cr.ac.utn.task_mngr"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cr.ac.utn.task_mngr"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.runtime)
    //implementation(libs.androidx.room.ktx)
    //implementation(libs.androidx.room.runtime)
    //implementation(libs.com.google.devtools.ksp.gradle.plugin)
    implementation(libs.material)
    //implementation(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    //compose
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.10.0")

    //hilt
    implementation("com.google.dagger:hilt-android:2.57.1")
    //ksp("com.google.dagger:hilt-android-compiler:2.57.1")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel-compose:1.4.0")

    //room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    //"ksp"("androidx.room:room-compiler:$roomVersion")
    //implementation("androidx.room3:room3-runtime:$roomVersion")
    //ksp("androidx.room3:room3-compiler:$roomVersion")
}