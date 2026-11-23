import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { stream -> load(stream) }
    }
}
val auddApiKey = localProperties.getProperty("audd.apiKey") ?: ""
val supabaseUrl = localProperties.getProperty("supabase.url") ?: ""
val supabaseAnonKey = localProperties.getProperty("supabase.anonKey") ?: ""
val updateManifestUrl = localProperties.getProperty("update.url")
    ?: "https://raw.githubusercontent.com/4lexzx/PoPMusic/main/native/update.json"
val youtubeClientId = localProperties.getProperty("youtube.clientId") ?: ""
val youtubeClientSecret = localProperties.getProperty("youtube.clientSecret") ?: ""
val spotifyClientId = localProperties.getProperty("spotify.clientId") ?: ""
val spotifyRedirectUri = localProperties.getProperty("spotify.redirectUri")
    ?: "http://127.0.0.1:5599/callback"
val youtubeRedirectUri = localProperties.getProperty("youtube.redirectUri")
    ?: "http://127.0.0.1:5599/callback"

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { stream -> load(stream) }
    }
}

android {
    namespace = "com.popmusic.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.popmusic.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 9
        versionName = "0.1.9"

        buildConfigField("String", "AUDD_API_KEY", "\"$auddApiKey\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "SUPABASE_REDIRECT_URL", "\"popmusic://auth-callback\"")
        buildConfigField("String", "UPDATE_MANIFEST_URL", "\"$updateManifestUrl\"")
        buildConfigField("String", "YOUTUBE_CLIENT_ID", "\"$youtubeClientId\"")
        buildConfigField("String", "YOUTUBE_CLIENT_SECRET", "\"$youtubeClientSecret\"")
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyClientId\"")
        buildConfigField("String", "MUSIC_REDIRECT_URI", "\"$spotifyRedirectUri\"")
        buildConfigField("String", "YOUTUBE_REDIRECT_URI", "\"$youtubeRedirectUri\"")
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystoreProperties.isNotEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("perf") {
            initWith(getByName("release"))
            isDebuggable = false
            applicationIdSuffix = ".perf"
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.newpipe.extractor)
    implementation(libs.okhttp)
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.profileinstaller)
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
