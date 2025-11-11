androidApplication {
    namespace = "org.example.app"

    dependencies {
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation(project(":utilities"))
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
        implementation("androidx.core:core-ktx:1.13.1")
        implementation("com.google.android.material:material:1.12.0")
    }
}
