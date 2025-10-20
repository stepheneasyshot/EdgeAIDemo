plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.edgeaidemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.edgeaidemo"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.1"

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
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {

    implementation(fileTree("libs").include("*.aar", "*.jar"))

    implementation(project(":llamacppbridge"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.kotlin.serialization)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.redfincommonhelper)

    implementation(libs.aicore)

    // LiteRT dependencies for Google Play services
    implementation(libs.play.services.tflite.java)
    // Optional: include LiteRT Support Library
    implementation(libs.play.services.tflite.support)

    // TensorFlow Lite
    implementation(libs.tflite)
    // 2. 推荐：用于模型元数据和实用程序的库
    implementation(libs.tflite.support)
    // 3. 可选：如果您需要特定的硬件加速（Delegate），请添加对应的独立依赖。例如，使用 GPU Delegate：
    implementation(libs.tflite.gpu)
}