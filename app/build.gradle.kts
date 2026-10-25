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
        val env = mapOf(
            "storeFile" to System.getenv("RELEASE_STORE_FILE"),
            "storePassword" to System.getenv("RELEASE_STORE_PASSWORD"),
            "keyAlias" to System.getenv("RELEASE_KEY_ALIAS"),
            "keyPassword" to System.getenv("RELEASE_KEY_PASSWORD"),
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
        versionCode = 5
        versionName = "2.0"
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
        release {
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
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // Android's built-in org.json is a stub in local unit tests.
    testImplementation("org.json:json:20240303")
}
