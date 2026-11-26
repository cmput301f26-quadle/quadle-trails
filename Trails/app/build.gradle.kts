import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)

}

// Secrets kept out of git: each developer puts them in their own local.properties.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.example.trails"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.quadletrails"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Map tiles for campsite pins (US 02.01.02). Uses the team's course key from
        // gradle.properties; a MAPTILER_KEY=... in local.properties overrides it.
        resValue(
            "string", "maptiler_key",
            localProps.getProperty("MAPTILER_KEY") ?: (project.findProperty("MAPTILER_KEY") as String? ?: "")
        )
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "TESTING_TOOLS", "true")
            versionNameSuffix = "-testing"
        }
        release {
            buildConfigField("boolean", "TESTING_TOOLS", "false")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true // needed for the maptiler_key resValue above
    }
    testOptions {
        unitTests.isReturnDefaultValues = true // added by chitkarsh on 11/05/2026
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.10.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.osmdroid.android) // OpenStreetMap map pins, US 02.01.02
    implementation(libs.zxing.core) // draws campsite QR codes, US 02.02.01 (Neaz)
    implementation(libs.zxing.android.embedded) // camera QR scanner, US 01.12.01 (Neaz)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test) // added by chitkarsh on 11/05/2026
    testImplementation(libs.mockito.core) // added by chitkarsh on 11/05/2026
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.compose.material.icons.extended) // added by chitkarsh on 10/29/2026
}
