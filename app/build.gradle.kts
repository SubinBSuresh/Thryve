import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android.plugin)
    alias(libs.plugins.ksp.plugin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.gms.google.services)
//    id("kotlin-serialization")
}

android.applicationVariants.all {
    outputs.all {
        val variantName = name
        val copyTaskName = "copy${variantName.capitalize()}Apk"

        tasks.register<Copy>(copyTaskName) {
            dependsOn(assembleProvider)

            from(outputFile)
            into("${rootProject.projectDir}/apk")

            rename {
                "Thryve-$variantName.apk"
            }
        }

        assembleProvider.get().finalizedBy(copyTaskName)
    }
}


val localProperties = Properties().apply {
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

android {
    namespace = "com.dutch.thryve"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dutch.thryve"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig fields for test credentials
        buildConfigField("String", "TEST_EMAIL", "\"${localProperties.getProperty("TEST_EMAIL")}\"")
        buildConfigField("String", "TEST_PASSWORD", "\"${localProperties.getProperty("TEST_PASSWORD")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY")}\"")
        buildConfigField("String", "OPENAI_SECRET_KEY", "\"${localProperties.getProperty("OPENAI_SECRET_KEY")}\"")
        buildConfigField("String", "OPENROUTER_SECRET_KEY", "\"${localProperties.getProperty("OPENROUTER_SECRET_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
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
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.places)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    //navigation
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    //room
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    //json
    implementation(libs.kotlinx.serialization.json)

    //icons
    implementation(libs.androidx.material.icons.extended)

    //okhttp
    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime.ktx)

    // Charting
    // implementation(libs.vico.core)
    // implementation(libs.vico.compose)
    // implementation(libs.vico.compose.m3)
}