import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = keystorePropertiesFile.takeIf { it.isFile }?.let { propertiesFile ->
    Properties().apply {
        propertiesFile.inputStream().use(::load)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.jacobgain.triprabbit"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.jacobgain.triprabbit"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    testOptions { unitTests.isIncludeAndroidResources = true }
    lint { checkTestSources = false }
    signingConfigs {
        keystoreProperties?.let { properties ->
            create("release") {
                storeFile = file(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
kapt { correctErrorTypes = true }

val verifyPlayPolicyRelease by tasks.registering {
    group = "verification"
    description = "Checks the merged release manifest against TripRabbit's Play policy declarations."
    dependsOn("processReleaseManifest")

    doLast {
        val manifest = layout.buildDirectory.file(
            "intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml"
        ).get().asFile
        check(manifest.isFile) { "Merged release manifest not found: $manifest" }

        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(manifest)
        val androidNamespace = "http://schemas.android.com/apk/res/android"

        val usesSdk = document.getElementsByTagName("uses-sdk").item(0)
        val targetSdk = usesSdk.attributes.getNamedItemNS(androidNamespace, "targetSdkVersion")
            ?.nodeValue?.toIntOrNull()
        check(targetSdk != null && targetSdk >= 36) {
            "Release targetSdk must be at least 36; found $targetSdk"
        }

        val platformPermissions = buildList {
            val permissions = document.getElementsByTagName("uses-permission")
            for (index in 0 until permissions.length) {
                val name = permissions.item(index).attributes
                    .getNamedItemNS(androidNamespace, "name")?.nodeValue.orEmpty()
                if (name.startsWith("android.permission.")) add(name)
            }
        }
        check(platformPermissions.isEmpty()) {
            "Platform permissions require a new Play/privacy review: $platformPermissions"
        }

        val application = document.getElementsByTagName("application").item(0)
        check(application.attributes.getNamedItemNS(androidNamespace, "allowBackup")?.nodeValue == "false") {
            "Release must keep Android backup disabled"
        }
        check(application.attributes.getNamedItemNS(androidNamespace, "dataExtractionRules")?.nodeValue == "@xml/data_extraction_rules") {
            "Release must apply the cloud/device-transfer exclusion rules"
        }
    }
}

tasks.named("check") { dependsOn(verifyPlayPolicyRelease) }
tasks.matching { it.name == "bundleRelease" }.configureEach {
    finalizedBy(verifyPlayPolicyRelease)
}
