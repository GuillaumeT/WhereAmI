plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "fr.troupel.whereami"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.troupel.whereami"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.0.1-alpha.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
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
        compose = true
    }

    assetPacks += listOf(":assetpack_waipack")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.asset.delivery.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.maplibre.android.sdk)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mapbox.sdk.turf)
    implementation(libs.konfetti.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.register<ComputeMinDistancesTask>("computeMinDistances") {
    countriesGeoJson =
        layout.projectDirectory.file("src/main/assets/ne_10m_admin_0_countries.geojson")
    outputJson = layout.buildDirectory.file("distances/all.json")
}

tasks.register<CheckCountriesUniqueIDTask>("checkCountriesUniqueID") {
    countriesGeoJsonFile =
        layout.projectDirectory.file("src/main/assets/ne_10m_admin_0_countries.geojson")
}