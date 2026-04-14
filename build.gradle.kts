// =============================================================
// google-services.json Setup Instructions
// =============================================================
//
// To enable Firebase services in this project, you must add a
// google-services.json configuration file:
//
// 1. Go to the Firebase Console: https://console.firebase.google.com/
// 2. Create a new Firebase project or select an existing one.
// 3. Click "Add app" and choose the Android platform.
// 4. Enter the package name: com.yourteam.nextstop
// 5. (Optional) Enter the app nickname: NextStop
// 6. (Optional) Provide the SHA-1 debug signing certificate.
// 7. Click "Register app" and download the google-services.json file.
// 8. Place the downloaded google-services.json file in the
//    app/ directory of this project:
//
//       NextStop/app/google-services.json
//
// 9. Sync Gradle and rebuild the project.
//
// NOTE: The google-services Gradle plugin is already applied in
//       app/build.gradle.kts via: alias(libs.plugins.google.services)
//
// WARNING: Do NOT commit google-services.json to public repositories.
//          Add it to your .gitignore if the repo is public.
//
// =============================================================

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.google.services) apply false
}
