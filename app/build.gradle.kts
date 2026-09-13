plugins {
    alias(libs.plugins.android.application)
    //alias(libs.plugins.kotlin.android)
    //id("com.google.devtools.ksp") // Necesario para procesar las anotaciones
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
    //implementation(libs.androidx.room.ktx)
    //implementation(libs.androidx.room.runtime)
    //implementation(libs.com.google.devtools.ksp.gradle.plugin)
    implementation(libs.material)
    //implementation(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    //room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    //"ksp"("androidx.room:room-compiler:$roomVersion")
    //implementation("androidx.room3:room3-runtime:$roomVersion")
    //ksp("androidx.room3:room3-compiler:$roomVersion")
}