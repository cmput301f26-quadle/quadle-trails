// The app's only activity: sets up edge-to-edge, identifies the device and shows TrailsApp.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
// Outstanding issues: None currently known.
// AI assistance: Used AI to help switch release builds to Firebase anonymous sign-in.
package com.example.trails

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.example.trails.data.device.FirebaseDeviceIdProvider
import com.example.trails.data.fake.CurrentUser
import com.example.trails.ui.navigation.TrailsApp
import com.example.trails.ui.profile.IdentityGate
import com.example.trails.ui.theme.TrailsTheme
import com.example.trails.ui.testing.TestingRoleGate

/** The app's only activity; every screen is a Compose destination in TrailsApp. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Release builds identify the user by device with Firebase anonymous sign-in
        // (US 01.09.01). Only once per process, so a rotation keeps the session.
        if (!BuildConfig.TESTING_TOOLS && CurrentUser.canUseSampleUsers) {
            CurrentUser.install(FirebaseDeviceIdProvider())
        }
        // The theme is always dark, so force light status/nav bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            TrailsTheme {
                if (BuildConfig.TESTING_TOOLS) {
                    // Debug: pick a sample profile, or "This device" for real sign-in.
                    TestingRoleGate { TrailsApp() }
                } else {
                    IdentityGate { TrailsApp() }
                }
            }
        }
    }
}
