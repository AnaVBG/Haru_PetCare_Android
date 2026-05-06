plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.dam2.haru_petcare"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.dam2.haru_petcare"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // --- Retrofit: cliente HTTP para consumir nuestra API REST ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    // --- Gson Converter: deserializa el JSON de la API a objetos Java ---
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // --- OkHttp Logging Interceptor: imprime en Logcat las peticiones HTTP ---
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // --- Glide: carga imágenes de URL (fotos de mascotas) de forma eficiente ---
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // --- Google Maps SDK (para PinMapa y AlertaPerdida) ---
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // --- Firebase (añadir google-services.json del proyecto Firebase) ---
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-messaging")   // FCM push notification


}