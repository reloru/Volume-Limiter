plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ciKeystore: String? = System.getenv("KEYSTORE_PATH")

android {
    namespace = "com.reloru.volumelimiter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.reloru.volumelimiter"
        minSdk = 26
        targetSdk = 34
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = "1.0.$versionCode"
    }

    signingConfigs {
        if (ciKeystore != null) {
            create("release") {
                storeFile = file(ciKeystore)
                storePassword = "volumelimiter"
                keyAlias = "volumelimiter"
                keyPassword = "volumelimiter"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (ciKeystore != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint { abortOnError = true }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
