package app.kiddo.feature.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.kiddo.core.database.BackupService
import app.kiddo.core.designsystem.component.Field
import app.kiddo.core.designsystem.component.FieldKind
import app.kiddo.core.designsystem.component.Group
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.ListRow
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.SheetText
import app.kiddo.core.designsystem.component.TextAction
import app.kiddo.core.designsystem.component.Title
import app.kiddo.core.designsystem.component.Toggle
import app.kiddo.core.navigation.Route
import app.kiddo.core.navigation.startOver
import app.kiddo.core.platform.rememberFileOpener
import app.kiddo.core.platform.rememberFileSaver
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.backup_export
import app.kiddo.core.resources.backup_keys
import app.kiddo.core.resources.backup_keys_note
import app.kiddo.core.resources.backup_not_saved
import app.kiddo.core.resources.backup_passphrase
import app.kiddo.core.resources.backup_passphrase_note
import app.kiddo.core.resources.backup_restore
import app.kiddo.core.resources.backup_restore_body
import app.kiddo.core.resources.backup_restore_go
import app.kiddo.core.resources.backup_restore_title
import app.kiddo.core.resources.backup_restored
import app.kiddo.core.resources.backup_saved
import app.kiddo.core.resources.backup_sub
import app.kiddo.core.resources.backup_title
import app.kiddo.core.resources.backup_wrong
import app.kiddo.core.resources.generated.KiddoGlyph
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.dsl.module

val backupModule = module { factory { BackupService(get(), get<app.kiddo.core.security.Vault>().secrets) } }

fun NavGraphBuilder.backupScreens(nav: NavController) {
    composable<Route.Backup> {
        val service: BackupService = koinInject()
        val overlay = LocalOverlay.current
        val scope = rememberCoroutineScope()
        var busy by remember { mutableStateOf(false) }
        val toast: (org.jetbrains.compose.resources.StringResource) -> Unit = { res -> scope.launch { overlay.toast(getString(res)) } }
        val save = rememberFileSaver { ok -> toast(if (ok) Res.string.backup_saved else Res.string.backup_not_saved) }
        val restoreTitle = stringResource(Res.string.backup_restore_title)
        val open = rememberFileOpener { bytes ->
            if (bytes == null) return@rememberFileOpener
            overlay.sheet(restoreTitle) {
                RestoreForm { passphrase ->
                    scope.launch {
                        if (service.restore(bytes, passphrase)) {
                            overlay.close()
                            overlay.toast(getString(Res.string.backup_restored))
                            nav.startOver(Route.Home())
                        } else {
                            overlay.toast(getString(Res.string.backup_wrong))
                        }
                    }
                }
            }
        }
        BackupScreen(busy, onBack = { nav.popBackStack() }, onExport = { passphrase, keys ->
            busy = true
            scope.launch {
                val bytes = service.export(passphrase, keys)
                busy = false
                save("kiddo-backup.kiddo", bytes)
            }
        }, onRestore = open)
    }
}

@Composable
fun BackupScreen(busy: Boolean, onBack: () -> Unit, onExport: (String, Boolean) -> Unit, onRestore: () -> Unit, modifier: Modifier = Modifier) {
    var keys by rememberSaveable { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    KiddoScreen(
        modifier,
        header = { NavBar(onBack = onBack) },
        footer = {
            Primary(stringResource(Res.string.backup_export), { onExport(passphrase, keys) }, enabled = passphrase.length >= MIN_PASSPHRASE, busy = busy, glyph = KiddoGlyph.Download)
            TextAction(stringResource(Res.string.backup_restore), onRestore)
        },
    ) {
        Title(stringResource(Res.string.backup_title), sub = stringResource(Res.string.backup_sub))
        val keysLabel = stringResource(Res.string.backup_keys)
        Group(note = stringResource(Res.string.backup_keys_note)) {
            ListRow(keysLabel, trailing = { Toggle(keys, { keys = it }, keysLabel) })
        }
        Field(passphrase, { passphrase = it }, label = stringResource(Res.string.backup_passphrase), kind = FieldKind.Secret, note = stringResource(Res.string.backup_passphrase_note))
    }
}

@Composable
private fun RestoreForm(onRestore: (String) -> Unit) {
    var passphrase by remember { mutableStateOf("") }
    SheetText(stringResource(Res.string.backup_restore_body))
    Field(passphrase, { passphrase = it }, label = stringResource(Res.string.backup_passphrase), kind = FieldKind.Secret)
    Primary(stringResource(Res.string.backup_restore_go), { onRestore(passphrase) }, enabled = passphrase.isNotEmpty())
}

private const val MIN_PASSPHRASE = 8
