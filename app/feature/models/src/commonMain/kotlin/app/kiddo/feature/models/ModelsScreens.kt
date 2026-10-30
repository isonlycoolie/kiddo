package app.kiddo.feature.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.component.Empty
import app.kiddo.core.designsystem.component.Field
import app.kiddo.core.designsystem.component.FieldKind
import app.kiddo.core.designsystem.component.Group
import app.kiddo.core.designsystem.component.Hairline
import app.kiddo.core.designsystem.component.KiddoIcon
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.ListRow
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.Note
import app.kiddo.core.designsystem.component.PickerField
import app.kiddo.core.designsystem.component.Pill
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.ProviderLogo
import app.kiddo.core.designsystem.component.TextAction
import app.kiddo.core.designsystem.component.Title
import app.kiddo.core.designsystem.component.message
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.designsystem.component.sub
import app.kiddo.core.model.AiProvider
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.ai_add_provider
import app.kiddo.core.resources.ai_add_sub
import app.kiddo.core.resources.ai_add_title
import app.kiddo.core.resources.ai_address
import app.kiddo.core.resources.ai_address_note
import app.kiddo.core.resources.ai_connected
import app.kiddo.core.resources.ai_edit_title
import app.kiddo.core.resources.ai_empty_body
import app.kiddo.core.resources.ai_empty_title
import app.kiddo.core.resources.ai_key
import app.kiddo.core.resources.ai_key_from
import app.kiddo.core.resources.ai_key_saved
import app.kiddo.core.resources.ai_model
import app.kiddo.core.resources.ai_model_pick
import app.kiddo.core.resources.ai_privacy
import app.kiddo.core.resources.ai_provider
import app.kiddo.core.resources.ai_remove
import app.kiddo.core.resources.ai_sealed
import app.kiddo.core.resources.ai_test_save
import app.kiddo.core.resources.ai_title
import app.kiddo.core.resources.ai_tokens
import app.kiddo.core.resources.ai_use
import app.kiddo.core.resources.ai_use_chat
import app.kiddo.core.resources.generated.KiddoGlyph
import org.jetbrains.compose.resources.stringResource

class AiActions(val onBack: () -> Unit, val onAdd: () -> Unit, val onOpen: (String) -> Unit, val onPickChat: () -> Unit)

@Composable
fun AiScreen(state: AiState, actions: AiActions, modifier: Modifier = Modifier) {
    KiddoScreen(modifier, header = { NavBar(title = stringResource(Res.string.ai_title), onBack = actions.onBack) }) {
        if (state.accounts.isEmpty()) {
            Empty(KiddoGlyph.Ai, stringResource(Res.string.ai_empty_title), body = stringResource(Res.string.ai_empty_body)) {
                Pill(stringResource(Res.string.ai_add_provider), actions.onAdd, glyph = KiddoGlyph.Add)
            }
            return@KiddoScreen
        }
        Group(title = stringResource(Res.string.ai_connected)) {
            state.accounts.forEachIndexed { i, account ->
                if (i > 0) Hairline()
                ListRow(
                    account.provider.label,
                    lead = { ProviderLogo(account.provider) },
                    sub = sub(stringResource(Res.string.ai_tokens, account.model, formatCount(account.tokensUsed))),
                    onClick = { actions.onOpen(account.id) },
                )
            }
        }
        Group(title = stringResource(Res.string.ai_use)) {
            ListRow(stringResource(Res.string.ai_use_chat), value = state.chat?.model, onClick = actions.onPickChat)
        }
        Group { ListRow(stringResource(Res.string.ai_add_provider), glyph = KiddoGlyph.Add, onClick = actions.onAdd) }
        Note(stringResource(Res.string.ai_privacy), glyph = KiddoGlyph.Shield)
    }
}

class AddActions(
    val onBack: () -> Unit,
    val onPickProvider: () -> Unit,
    val onKey: (String) -> Unit,
    val onBaseUrl: (String) -> Unit,
    val onModel: (String) -> Unit,
    val onPickModel: () -> Unit,
    val onSave: () -> Unit,
    val onRemove: () -> Unit,
)

