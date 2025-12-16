import com.android.build.api.dsl.ApplicationExtension
import java.io.FileInputStream
import java.io.FileOutputStream
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
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

configure<ApplicationExtension> {
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

    buildFeatures {
        compose = true
    }
}

base {
    archivesName.set("cresto-alpha${vCode}")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
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
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.androidx.compose.foundation.layout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.haze)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.generativeai)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mmkv)
    implementation(libs.capsule)
    implementation(libs.backdrop)
}

tasks.register("incrementVersionCode") {
    val versionPropsFile = layout.projectDirectory.file("version.properties").asFile

    outputs.upToDateWhen { false }

    doLast {
        if (versionPropsFile.exists()) {
            val props = Properties()

            FileInputStream(versionPropsFile).use { stream ->
                props.load(stream)
            }

            val currentCode = props.getProperty("VERSION_CODE", "1").toInt()
            val newCode = currentCode + 1
            props.setProperty("VERSION_CODE", newCode.toString())

            FileOutputStream(versionPropsFile).use { stream ->
                props.store(stream, "Version properties updated")
            }

            println("✅ Version code incremented to $newCode")
        } else {
            println("⚠️ version.properties not found, skipping increment.")
        }
    }
}

tasks.configureEach {
    val taskName = name.lowercase()
    val isRelease = taskName.contains("release")
    val isPackageTask = taskName.contains("assemble") || taskName.contains("bundle")

    if (isRelease && isPackageTask) {
        dependsOn("incrementVersionCode")
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}