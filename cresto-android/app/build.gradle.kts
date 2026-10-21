plugins { id("com.android.application") }
android {
    namespace = "com.cresto.reference"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.cresto.reference"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-demo"
    }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies { implementation("androidx.webkit:webkit:1.12.1") }