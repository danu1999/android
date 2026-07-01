plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.posbah.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.posbah.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 125
        versionName = "2.19.18"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    
        // Inject Google OAuth Web Client ID at build time
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"119416648055-hil4u0bmuqffcn2u1f6se66h1lhdiugr.apps.googleusercontent.com\""
        )
        // Inject Admin Auth Token at build time, allowing local overrides in local.properties
        val localToken = project.findProperty("adminAuthToken") as? String ?: "Bearer BahteraMigrate123!"
        buildConfigField("String", "ADMIN_AUTH_TOKEN", "\"$localToken\"")
        buildConfigField("boolean", "ENFORCE_INTEGRITY", "true")
    }

    signingConfigs {
        // Release signing must be set in local.properties by the developer.
        create("release") {
            val keystoreFile = file("${rootProject.projectDir}/keystore/release.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("POSBAH_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("POSBAH_KEY_ALIAS") ?: "posbah"
                keyPassword = System.getenv("POSBAH_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "DEBUG_MODE", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            buildConfigField("boolean", "DEBUG_MODE", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use release keystore if present, otherwise debug (for testing).
            val keystoreFile = file("${rootProject.projectDir}/keystore/release.jks")
            signingConfig = if (keystoreFile.exists()) {
                signingConfigs.findByName("release")
            } else {
                signingConfigs.findByName("debug")
            }
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "/META-INF/DEPENDENCIES"
            )
        }
    }

    // === Auto-naming APK output ===
    // Release: posbah-v2.16.0.apk
    // Debug:   posbah-v2.16.0-debug.apk
    applicationVariants.all {
        val variant = this
        val versionName = variant.versionName
        outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = if (variant.buildType.isDebuggable) {
                    if (versionName.endsWith("-debug")) "posbah-v$versionName.apk"
                    else "posbah-v$versionName-debug.apk"
                } else {
                    "posbah-v$versionName.apk"
                }
            }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Networking — OkHttp + Retrofit (Full Online mode)
    implementation(libs.okhttp)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // AndroidX & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.material)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room + SQLCipher — REMOVED (migrated to Full Online mode)
    // implementation(libs.androidx.room.runtime)
    // implementation(libs.androidx.room.ktx)
    // ksp(libs.androidx.room.compiler)
    // implementation(libs.sqlcipher.android)
    // implementation(libs.androidx.sqlite.ktx)

    // Coroutines & Serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    // Security
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.play.integrity)

    // Google Sign-In (Credential Manager)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)
    implementation(libs.auth0.jwtdecode)

    // Desugaring (java.time on minSdk 24)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Coil — image loading ringan untuk preview foto nota bahan baku
    implementation(libs.coil.compose)

    // ExifInterface — baca rotasi EXIF foto kamera (orientasi portrait/landscape)
    implementation(libs.androidx.exifinterface)

    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Google ML Kit Barcode Scanning
    implementation(libs.mlkit.barcode.scanning)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
