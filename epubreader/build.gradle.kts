plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.parcelize")
}

android {
    namespace = "com.epubreader.android"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            consumerProguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.7"
    }

    packaging {
        resources.excludes.add("META-INF/*")
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            res.srcDirs("src/main/res")
            assets.srcDirs("src/main/assets")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.5.7")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //==================== Compose ====================//
    implementation("androidx.activity:activity-compose:1.7.1")

    //==================== Readium ====================//
    implementation("org.readium.kotlin-toolkit:readium-shared:2.3.0")
    implementation("org.readium.kotlin-toolkit:readium-streamer:2.3.0")
    implementation("org.readium.kotlin-toolkit:readium-navigator:2.3.0")
    implementation("org.readium.kotlin-toolkit:readium-opds:2.3.0")
    implementation("org.readium.kotlin-toolkit:readium-lcp:2.3.0")

    //==================== Database ====================//
    val roomVersion = "2.5.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    implementation("joda-time:joda-time:2.12.5")

    //==================== Logging ====================//
    implementation("com.jakewharton.timber:timber:5.0.1")
}