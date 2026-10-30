package app.kiddo.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

// The only activity; every screen is a Compose destination in KiddoApp.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Secure until settings say otherwise, so the first frame never reaches the app switcher.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent { KiddoApp() }
    }
}
