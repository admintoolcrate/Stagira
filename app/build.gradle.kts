import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/** Upload-key settings from keystore.properties, else from RELEASE_* environment variables. */
val releaseSigning: Map<String, String>?
    get() {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) {
            val p = Properties().apply { file.inputStream().use { load(it) } }
            return listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
                .associateWith { p.getProperty(it) ?: error("keystore.properties is missing $it") }
        }
        // Trimmed: values pasted into GitHub secrets easily pick up a stray space or line break.
        val env = mapOf(
            "storeFile" to System.getenv("RELEASE_STORE_FILE")?.trim(),
            "storePassword" to System.getenv("RELEASE_STORE_PASSWORD")?.trim(),
            "keyAlias" to System.getenv("RELEASE_KEY_ALIAS")?.trim(),
            "keyPassword" to System.getenv("RELEASE_KEY_PASSWORD")?.trim(),
        )
        return if (env.values.all { !it.isNullOrEmpty() }) env.mapValues { it.value!! } else null
    }

android {
    namespace = "com.stagirite.ledger"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.stagirite.ledger"
        minSdk = 26
        targetSdk = 36
        // CI builds number themselves from the run (beta 57 is version 1057), so every upload to
        // Play has a new, higher code. Builds made elsewhere use the fixed fallback.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.let { 1000 + it } ?: 12
        versionName = "3.3"

        // The CI run number, which names each beta release ("beta-57"); 0 for builds made elsewhere.
        buildConfigField("int", "BUILD_NUMBER", (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0).toString())
    }

    signingConfigs {
        // A fixed debug key, so every debug build has the same SHA-1 to register
        // with Google for sign-in. Debug only; never use it for a release.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Your upload key, from keystore.properties (never committed) or, in CI,
        // from environment variables. Without either, release builds are unsigned.
        releaseSigning?.let { s ->
            create("release") {
                storeFile = file(s.getValue("storeFile"))
                storePassword = s.getValue("storePassword")
                keyAlias = s.getValue("keyAlias")
                keyPassword = s.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Test builds look for a newer beta on GitHub; Play builds never do.
            buildConfigField("boolean", "BETA_UPDATES", "true")
        }
        release {
            buildConfigField("boolean", "BETA_UPDATES", "false")
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    // The Goal widget keeps its chosen goal in Glance's Preferences state.
    implementation("androidx.datastore:datastore-preferences-core:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    // Play's review prompt and in-app updates (Play builds only; see support/PlayServices.kt).
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // Android's built-in org.json is a stub in local unit tests.
    testImplementation("org.json:json:20240303")
}
