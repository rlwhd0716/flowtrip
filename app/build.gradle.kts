import java.net.URI
import java.util.Properties
import java.util.concurrent.TimeUnit

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val pleosSdkEnabled = providers.gradleProperty("pleosSdk").map { it.toBooleanStrict() }.orElse(false).get()
// Local debug endpoint only. The Kakao key is read by the proxy, never by Gradle or the APK.
val localPlaceSettings = Properties().apply {
    val file = rootProject.file("place-proxy.local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val debugPlaceProxy = providers.gradleProperty("debugPlaceProxyUrl")
    .orElse(localPlaceSettings.getProperty("debugPlaceProxyUrl", "")).get()
val autoStartPlaceProxy = providers.gradleProperty("autoStartPlaceProxy")
    .orElse(localPlaceSettings.getProperty("autoStartPlaceProxy", "false")).get().toBooleanStrict()
if (debugPlaceProxy.isNotBlank()) {
    val uri = URI(debugPlaceProxy)
    require(uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null &&
        (uri.scheme == "https" || (uri.scheme == "http" && uri.host in listOf("10.0.2.2", "127.0.0.1", "localhost")))) {
        "Debug place proxy must use HTTPS or a local emulator/loopback HTTP address"
    }
}

// Local opt-in for Android Studio Run/Debug's normal Gradle build step.
// No outputs: perform the cheap health check even when the APK is up-to-date.
val debugProxyUri = debugPlaceProxy.takeIf { it.isNotBlank() }?.let { URI(it) }
val proxyWorkspace = rootProject.projectDir
val proxyStartupScript = rootProject.file("tools/place-proxy/start.ps1").absolutePath
val startLocalPlaceProxy = tasks.register("startLocalPlaceProxy") {
    group = "flowtrip"
    description = "Starts the local development place proxy before a Debug build (local opt-in only)."
    onlyIf {
        autoStartPlaceProxy && System.getProperty("os.name").startsWith("Windows") &&
            debugProxyUri?.scheme == "http" && debugProxyUri.host in listOf("10.0.2.2", "127.0.0.1", "localhost")
    }
    val port = debugProxyUri?.port?.takeIf { it > 0 } ?: 8787
    val arguments = mutableListOf("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "RemoteSigned",
        "-File", proxyStartupScript, "-Port", port.toString())
    if (debugProxyUri?.host == "10.0.2.2") arguments.add("-ServerOnly")
    // Gradle's Windows Exec job waits for descendants. Use a direct launcher with file-backed
    // handles so the long-lived server is independent of the build task's managed process tree.
    doLast {
        val log = proxyWorkspace.resolve("tools/place-proxy/proxy-startup.log")
        val process = ProcessBuilder(arguments).directory(proxyWorkspace).redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(log)).start()
        process.outputStream.close()
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroy()
            throw GradleException("Local place proxy startup timed out. Check tools/place-proxy/proxy-startup.log")
        }
        if (process.exitValue() != 0) throw GradleException("Local place proxy startup failed. Check tools/place-proxy/proxy-startup.log")
        logger.lifecycle("Local place proxy ready on port $port")
    }
}
tasks.matching { it.name == "preDebugBuild" }.configureEach { dependsOn(startLocalPlaceProxy) }

android {
    useLibrary("android.car", false)
    namespace = "com.github.flowtrip"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        val placeMode = providers.gradleProperty("placeDataSource").orElse("mock").get()
        require(placeMode in listOf("mock", "kakao")) { "placeDataSource must be mock or kakao" }
        val proxy = providers.gradleProperty("placeProxyUrl").orElse("").get()
        require(proxy.isEmpty() || (proxy.startsWith("https://") && proxy.matches(Regex("[A-Za-z0-9:/._~-]+")))) { "Use an HTTPS proxy URL without credentials or query" }
        buildConfigField("boolean", "REAL_PLACES", (placeMode == "kakao").toString())
        buildConfigField("boolean", "ALLOW_LOCAL_PLACE_PROXY", "false")
        buildConfigField("String", "PLACE_PROXY_URL", "\"$proxy\"")
        buildConfigField("boolean", "PLEOS_COORDINATE_ROUTING", providers.gradleProperty("pleosCoordinateRouting").orElse("false").get().toBooleanStrict().toString())
        buildConfigField("boolean", "PLEOS_SDK_ENABLED", pleosSdkEnabled.toString())
        applicationId = "com.github.flowtrip"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            if (debugPlaceProxy.isNotBlank()) {
                buildConfigField("boolean", "REAL_PLACES", "true")
                buildConfigField("String", "PLACE_PROXY_URL", "\"$debugPlaceProxy\"")
                buildConfigField("boolean", "ALLOW_LOCAL_PLACE_PROXY", "true")
            }
        }
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
        compose = true
        buildConfig = true
    }
    sourceSets.getByName("main").kotlin.directories.add(if (pleosSdkEnabled) "src/pleos/java" else "src/mock/java")
    if (pleosSdkEnabled) {
        sourceSets.getByName("debug").manifest.srcFile("src/pleos/AndroidManifest.xml")
        sourceSets.getByName("release").manifest.srcFile("src/pleos/AndroidManifest.xml")
    }
}

dependencies {
    if (pleosSdkEnabled) {
        implementation(libs.pleos.navi)
        implementation(libs.pleos.fused.location)
    }
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
