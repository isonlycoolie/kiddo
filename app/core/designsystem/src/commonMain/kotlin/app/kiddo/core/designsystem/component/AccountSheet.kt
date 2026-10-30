package app.kiddo.core.designsystem.component

import androidx.compose.ui.unit.dp
import app.kiddo.core.model.ProviderAccount

// Chooses which connected model the chat uses; shared with the chat's own model switcher.
fun Overlay.accountSheet(title: String, accounts: List<ProviderAccount>, current: String?, onPick: (String) -> Unit) = sheet(title) {
    SheetGroup {
        accounts.forEach { a ->
            SheetRow(a.model, { onPick(a.id).also { close() } }, lead = { ProviderLogo(a.provider, size = 34.dp) }, sub = a.provider.label, picked = a.id == current)
        }
    }
}
