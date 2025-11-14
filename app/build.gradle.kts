import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

fun getVersionProps(project: Project): Properties {
    val versionPropsFile = project.file("version.properties")
    if (!versionPropsFile.canRead()) {
        throw GradleException("Could not read version.properties!")
    }
    return Properties().apply {
        load(FileInputStream(versionPropsFile))
    }
}

val versionProps = getVersionProps(project)
val vCode = versionProps["VERSION_CODE"].toString().toInt()
val vName = versionProps["VERSION_NAME"].toString()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nevoit.cresto"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.nevoit.cresto"
        minSdk = 31
        targetSdk = 36
        versionCode = vCode
        versionName = vName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.add("arm64-v8a")
        }

        setProperty("archivesBaseName", "cresto-alpha${versionCode}")
    }

    applicationVariants.all {
        outputs.map { it as BaseVariantOutputImpl }.forEach { output ->
            val originalFileName = output.outputFileName
            val newName = originalFileName.replace(
                ".apk",
                ".APK"
            )

            output.outputFileName = newName
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.ui)
    implementation(libs.androidx.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.room:room-runtime:2.8.2")
    ksp("androidx.room:room-compiler:2.8.2")
    implementation("androidx.room:room-ktx:2.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("dev.chrisbanes.haze:haze:1.6.10")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.tencent:mmkv:2.2.4")
    implementation("io.github.kyant0:capsule:2.1.1")
    implementation("io.github.kyant0:backdrop:1.0.0")
}

tasks.register("incrementVersionCode") {
    doLast {
        val props = getVersionProps(project)
        val currentCode = props["VERSION_CODE"].toString().toInt()
        val newCode = currentCode + 1
        props["VERSION_CODE"] = newCode.toString()

        val versionPropsFile = project.file("version.properties")
        versionPropsFile.writer().use { writer ->
            props.store(writer, "Version properties updated")
        }
        println("Version code incremented to $newCode")
    }
}

tasks.whenTaskAdded {
    if (name.contains("assemble") || name.contains("bundle")) {
        if (name.lowercase(Locale.getDefault()).contains("release")) {
            dependsOn("incrementVersionCode")
        }
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}