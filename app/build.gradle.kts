plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sunpodder.relay"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sunpodder.relay"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Vector drawable support
        vectorDrawables.useSupportLibrary = true
        
        // Exclude unused native libraries
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Additional optimizations
            ndk {
                debugSymbolLevel = "NONE"
            }
        }
        
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
        }

        create("debugOptimized") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = true
            
            // Use optimized ProGuard rules for debug builds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-debug-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    // Disable unused build features
    buildFeatures {
        buildConfig = false
        viewBinding = false
        dataBinding = false
        compose = false
        mlModelBinding = false
        prefab = false
        renderScript = false
        resValues = false
        shaders = false
    }
    
    // Packaging options to exclude unnecessary files
    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/gradle/incremental.annotation.processors"
            )
        }
    }
    
    // Bundle configuration for app bundles
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}

dependencies {
    // Use more specific AndroidX dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Use lighter Material Components
    implementation("com.google.android.material:material:1.11.0")
    
    // Exclude unused dependencies and transitive dependencies
    configurations.all {
        exclude(group = "androidx.lifecycle", module = "lifecycle-viewmodel-ktx")
        exclude(group = "androidx.lifecycle", module = "lifecycle-livedata-ktx")
        exclude(group = "androidx.navigation", module = "navigation-runtime-ktx")
        exclude(group = "androidx.navigation", module = "navigation-ui-ktx")
    }
    
    // Test dependencies (these don't affect release size)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}