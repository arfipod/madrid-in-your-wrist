import java.time.Instant

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.arfipod.wearosplayground"
    compileSdk = providers.gradleProperty("android.compileSdk").get().toInt()

    defaultConfig {
        applicationId = "com.arfipod.wearosplayground"
        minSdk = providers.gradleProperty("android.minSdk").get().toInt()
        targetSdk = providers.gradleProperty("android.targetSdk").get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        val napApiKey = providers.gradleProperty("nap.apiKey")
            .orElse(providers.environmentVariable("NAP_API_KEY"))
            .orElse("")
            .get()
        val emtClientId = providers.gradleProperty("emt.clientId")
            .orElse(providers.environmentVariable("EMT_CLIENT_ID"))
            .orElse("")
            .get()
        val emtPassKey = providers.gradleProperty("emt.passKey")
            .orElse(providers.environmentVariable("EMT_PASS_KEY"))
            .orElse("")
            .get()

        buildConfigField(
            "String",
            "BUILD_TIMESTAMP",
            "\"${Instant.now()}\""
        )
        buildConfigField(
            "String",
            "NAP_API_KEY",
            "\"${napApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
        buildConfigField(
            "String",
            "EMT_CLIENT_ID",
            "\"${emtClientId.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
        buildConfigField(
            "String",
            "EMT_PASS_KEY",
            "\"${emtPassKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.05.00")

    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.wear.compose:compose-material3:1.6.2")
    implementation("androidx.wear.tiles:tiles:1.6.0")
    implementation("androidx.wear.protolayout:protolayout:1.4.0")
    implementation("androidx.wear.protolayout:protolayout-material3:1.4.0")
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0")
    debugImplementation("androidx.wear.tiles:tiles-renderer:1.6.0")

    testImplementation("junit:junit:4.13.2")
}
