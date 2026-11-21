import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

evaluationDependsOn(":app")
val appAndroid = project(":app").extensions.getByType<ApplicationExtension>()

android {
    namespace = "com.kyant.shapes"
    compileSdk = appAndroid.compileSdk
    compileSdkMinor = appAndroid.compileSdkMinor

    defaultConfig {
        minSdk = appAndroid.defaultConfig.minSdk
    }

    compileOptions {
        sourceCompatibility = appAndroid.compileOptions.sourceCompatibility
        targetCompatibility = appAndroid.compileOptions.targetCompatibility
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.util)
}
