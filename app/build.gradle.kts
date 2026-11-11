plugins { id("com.android.application") }
android {
    namespace = "io.github.mitchelded.twspopup"
    compileSdk = 34
    defaultConfig {
        applicationId = "io.github.mitchelded.twspopup"
        minSdk = 29; targetSdk = 34; versionCode = 8; versionName = "1.1-dev"
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"; keyAlias = "key"; keyPassword = "android"
        }
        val signingVariables = listOf("ANYBUDS_SIGNING_STORE_FILE", "ANYBUDS_SIGNING_STORE_PASSWORD",
            "ANYBUDS_SIGNING_KEY_ALIAS", "ANYBUDS_SIGNING_KEY_PASSWORD")
        val signingValues = signingVariables.map { providers.environmentVariable(it).orNull }
        require(signingValues.all { it.isNullOrBlank() } || signingValues.all { !it.isNullOrBlank() }) {
            "Release signing requires all four ANYBUDS_SIGNING_* environment variables."
        }
        if (signingValues.all { !it.isNullOrBlank() }) {
            create("configuredRelease") {
                storeFile = file(signingValues[0]!!)
                storePassword = signingValues[1]
                keyAlias = signingValues[2]
                keyPassword = signingValues[3]
            }
        }
    }
    buildTypes.getByName("release") {
        signingConfig = signingConfigs.findByName("configuredRelease")
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    testImplementation("junit:junit:4.13.2")
}
