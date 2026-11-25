plugins {
    id("com.android.application")
}

android {
    namespace = "com.mzlhsc.orpl.admin"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mzlhsc.orpl.admin"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // 默认后端地址：Android 模拟器访问宿主机 127.0.0.1:8080 的约定地址
        // 正式部署时用 -PorplBaseUrl=http://服务器IP/admin/ 覆盖，无需改代码
        val orplBaseUrl = (project.findProperty("orplBaseUrl") as String?)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "http://10.0.2.2:8080/admin/"
        buildConfigField("String", "DEFAULT_BASE_URL", "\"$orplBaseUrl\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
