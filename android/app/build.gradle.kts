import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// La clave de Google Maps y la URL del servidor se leen de local.properties,
// que NO se sube a git. Copia local.properties.example y rellénalo.
val local = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val mapsKey: String = local.getProperty("MAPS_API_KEY", "")
val urlServidor: String = local.getProperty("URL_SERVIDOR", "http://192.168.1.100:8080")

android {
    namespace = "com.nodia.respira"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nodia.respira"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
        buildConfigField("boolean", "HAY_CLAVE_MAPS", (mapsKey.isNotBlank()).toString())
        buildConfigField("String", "URL_SERVIDOR", "\"$urlServidor\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
