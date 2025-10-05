import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // 🔥 Required for Firebase
}

android {
    namespace = "com.MIT.harisharma"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.MIT.harisharma"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

		// Expose OpenAI key from local.properties to BuildConfig
		val props = Properties()
		val propsFile = rootProject.file("local.properties")
		if (propsFile.exists()) {
			props.load(propsFile.inputStream())
		}
		val openAiKey = props.getProperty("OPENAI_API_KEY", "")
		buildConfigField("String", "OPENAI_API_KEY", "\"$openAiKey\"")

		val geminiKey = props.getProperty("GEMINI_API_KEY", "")
		buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")

		// Allow overriding the Gemini model from local.properties; default to a broadly available text model
		val geminiModel = props.getProperty("GEMINI_MODEL", "gemini-pro-latest")
		buildConfigField("String", "GEMINI_MODEL", "\"$geminiModel\"")
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

    buildFeatures {
        viewBinding = true // Enable ViewBinding for better UI handling
        buildConfig = true
    }
}

dependencies {
    // ✅ Core Android UI dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ✅ Additional core Android dependencies
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // ✅ Firebase BoM (Latest version - manages all Firebase versions automatically)
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))

    // 🔥 Firebase dependencies (versions managed by BoM)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // 📱 Phone Authentication (for OTP-based password reset)
    implementation("com.google.firebase:firebase-auth")

    // 🌐 Retrofit + JSON Converter (for OpenAI API integration)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 📱 ViewPager2 (for walkthrough screens)
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // 🎯 Dots Indicator (for walkthrough page indicators)
    implementation("com.tbuonomo:dotsindicator:5.0")

    // 🎨 Material Design Components (Latest version)
    implementation("com.google.android.material:material:1.12.0")

    // 📱 Fragment support for ViewPager2
    implementation("androidx.fragment:fragment:1.7.1")

    // 🔧 Lifecycle components (for proper activity/fragment lifecycle management)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.3")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.3")

    // 🎭 Animation support
    implementation("androidx.transition:transition:1.5.0")

    // 🖼️ Image loading (optional - for profile pictures or app icons)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // 📱 Safe Args (for navigation between activities)
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // 🔐 Biometric authentication (optional - for enhanced security)
    implementation("androidx.biometric:biometric:1.1.0")

    // 📊 Progress indicators
    implementation("com.google.android.material:material:1.12.0")

    // 🎯 SwipeRefreshLayout (for pull-to-refresh in chat)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // ✅ Testing dependencies
    testImplementation(libs.junit)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // 🧪 Firebase Testing (optional)
    testImplementation("com.google.firebase:firebase-auth")
    androidTestImplementation("com.google.firebase:firebase-auth")

    // 📱 UI Testing support
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // 🎭 Animation testing
    debugImplementation("androidx.fragment:fragment-testing:1.7.1")
}
