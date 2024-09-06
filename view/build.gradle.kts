plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.gonodono.bda.view"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    publishing {
        singleVariant("release")
    }
    sourceSets {
        named("main") {
            java.srcDir("src/main/kotlin")
        }
    }
}

tasks {
    register<Jar>("withSourcesJar") {
        archiveClassifier.set("sources")
        from(android.sourceSets.getByName("main").java.srcDirs)
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifact(tasks.named<Jar>("withSourcesJar"))
                groupId = "com.gonodono.bda"
                artifactId = "view"
                version = findProperty("library.version").toString()
            }
        }
    }
}

dependencies {

    api(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material.views)
}