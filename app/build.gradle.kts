plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("org.jetbrains.kotlinx.atomicfu") version "0.27.0"
}

android {
    namespace = "io.groovin.mcpsample"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.groovin.mcpsample"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        compose = true
    }
    //TODO : error fix용으로 추가한 것임
    packaging {
        resources {
            pickFirsts.add("META-INF/DEPENDENCIES")
        }
    }
}

dependencies {

    //androidx
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    //kotlinx
    implementation(libs.kotlinx.collections.immutable)

    //koin
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    //ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.serialization)

    //room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // mcp
    implementation(libs.mcp.sdk)
    implementation(libs.atomicfu)

    // anthropic claude
    implementation(libs.anthropic.java)

    // open ai
    implementation(libs.openai.java)

    // gemini (deprecated version)
    implementation(libs.generativeai.google)

    // groovin.io EncryptHelper, permX
    implementation(libs.encrypthelper)
    implementation(libs.permx)

    //markdown
    implementation(libs.richtext.markdown)
    implementation(libs.richtext.commonmark)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
