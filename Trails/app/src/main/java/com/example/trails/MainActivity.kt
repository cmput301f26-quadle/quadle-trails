// The app's only activity: sets up edge-to-edge and shows TrailsApp.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.example.trails.ui.navigation.TrailsApp
import com.example.trails.ui.theme.TrailsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The theme is always dark, so force light status/nav bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            TrailsTheme { TrailsApp() }
        }
    }
}
