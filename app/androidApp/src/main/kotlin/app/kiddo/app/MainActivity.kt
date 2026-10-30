package app.kiddo.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity

// The only activity; every screen is a Compose destination in KiddoApp. A FragmentActivity so the AndroidX
// biometric prompt can attach to it.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Secure until settings say otherwise, so the first frame never reaches the app switcher.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent { KiddoApp() }
    }
}
