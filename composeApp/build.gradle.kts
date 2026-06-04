plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    jvm("desktop")
    

    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.material)
            implementation(libs.google.code.gson)
            implementation(libs.mlkit.objectdetection)
            implementation(libs.mlkit.imagelabeling)
            implementation(libs.androidx.work.runtime.ktx)
        }


        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "kr.ac.pcu.aifinder.composeApp"
    compileSdk = 36
    defaultConfig {
        applicationId = "kr.ac.pcu.aifinder"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.desktop {
    application {
        mainClass = "kr.ac.pcu.aifinder.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "AIfinder"
            packageVersion = "1.0.0"
        }
    }
}
