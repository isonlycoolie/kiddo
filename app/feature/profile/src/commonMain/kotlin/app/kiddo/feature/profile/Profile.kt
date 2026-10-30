package app.kiddo.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.kiddo.core.database.CvStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.Avatar
import app.kiddo.core.designsystem.component.Field
import app.kiddo.core.designsystem.component.FieldKind
import app.kiddo.core.designsystem.component.Group
import app.kiddo.core.designsystem.component.Hairline
import app.kiddo.core.designsystem.component.Info
import app.kiddo.core.designsystem.component.KiddoIcon
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.ListRow
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.SheetGroup
import app.kiddo.core.designsystem.component.SheetRow
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.model.Cv
import app.kiddo.core.model.Profile
import app.kiddo.core.model.Signature
import app.kiddo.core.model.Tone
import app.kiddo.core.model.Workspace
import app.kiddo.core.navigation.Route
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_edit
import app.kiddo.core.resources.action_save
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.profile_default_cv
import app.kiddo.core.resources.profile_email
import app.kiddo.core.resources.profile_headline
import app.kiddo.core.resources.profile_in_every
import app.kiddo.core.resources.profile_linkedin
import app.kiddo.core.resources.profile_name
import app.kiddo.core.resources.profile_phone
import app.kiddo.core.resources.profile_portfolio
import app.kiddo.core.resources.profile_signature
import app.kiddo.core.resources.profile_stored
import app.kiddo.core.resources.profile_title
import app.kiddo.core.resources.profile_tone
import app.kiddo.core.resources.profile_writing
import app.kiddo.core.resources.profile_writing_note
import app.kiddo.core.resources.settings_none
import app.kiddo.core.resources.signature_name
import app.kiddo.core.resources.signature_name_phone
import app.kiddo.core.resources.tone_brief
import app.kiddo.core.resources.tone_formal
import app.kiddo.core.resources.tone_friendly
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

data class ProfileState(val workspace: Workspace, val profile: Profile, val defaultCv: Cv?)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(private val workspaces: WorkspaceStore, cvs: CvStore) : ViewModel() {
    val state: StateFlow<ProfileState?> = workspaces.workspace.filterNotNull().flatMapLatest { ws ->
        combine(workspaces.profile, cvs.cvs(ws.id)) { p, list -> ProfileState(ws, p, list.firstOrNull { it.isDefault }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER), null)

    fun save(workspace: Workspace, profile: Profile) {
        viewModelScope.launch { workspaces.save(workspace, profile) }
    }
}

val profileModule = module { viewModelOf(::ProfileViewModel) }

fun NavGraphBuilder.profileScreens(nav: NavController) {
    composable<Route.Profile> {
        val viewModel: ProfileViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val overlay = LocalOverlay.current
        val toneTitle = stringResource(Res.string.profile_tone)
        val signatureTitle = stringResource(Res.string.profile_signature)
        state?.let { s ->
            ProfileScreen(
                s,
                onBack = { nav.popBackStack() },
                onSave = viewModel::save,
                onTone = {
                    overlay.sheet(toneTitle) {
                        SheetGroup {
                            Tone.entries.forEach { t ->
                                SheetRow(stringResource(toneLabel(t)), { viewModel.save(s.workspace, s.profile.copy(tone = t)).also { overlay.close() } }, picked = t == s.profile.tone)
                            }
                        }
                    }
                },
                onSignature = {
                    overlay.sheet(signatureTitle) {
                        SheetGroup {
                            Signature.entries.forEach { g ->
                                SheetRow(stringResource(signatureLabel(g)), { viewModel.save(s.workspace, s.profile.copy(signature = g)).also { overlay.close() } }, picked = g == s.profile.signature)
                            }
                        }
                    }
                },
                onDefaultCv = { nav.navigate(Route.Cvs) },
            )
        }
    }
}

// Read first; Edit turns the details into fields, Save turns them back.
@Composable
fun ProfileScreen(
    state: ProfileState,
    onBack: () -> Unit,
    onSave: (Workspace, Profile) -> Unit,
    onTone: () -> Unit,
    onSignature: () -> Unit,
    onDefaultCv: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable(editing) { mutableStateOf(state.workspace.name) }
    var email by rememberSaveable(editing) { mutableStateOf(state.workspace.email) }
    var headline by rememberSaveable(editing) { mutableStateOf(state.profile.headline) }
    var phone by rememberSaveable(editing) { mutableStateOf(state.profile.phone) }
    var portfolio by rememberSaveable(editing) { mutableStateOf(state.profile.portfolio) }
    var linkedin by rememberSaveable(editing) { mutableStateOf(state.profile.linkedin) }
    val action = stringResource(if (editing) Res.string.action_save else Res.string.action_edit)
    KiddoScreen(
        modifier,
        header = {
            NavBar(title = stringResource(Res.string.profile_title).takeIf { editing }, onBack = onBack) {
                BasicText(
                    action,
                    style = Kiddo.type.bodyStrong.copy(color = Kiddo.colors.fg),
                    modifier = Modifier.pressable({
                        if (editing) {
                            onSave(
                                state.workspace.copy(name = name.ifBlank { state.workspace.name }, email = email.ifBlank { state.workspace.email }),
                                state.profile.copy(headline = headline, phone = phone, portfolio = portfolio, linkedin = linkedin),
                            )
                        }
                        editing = !editing
                    }, pressedAlpha = 0.5f).padding(12.dp),
                )
            }
        },
    ) {
        if (editing) {
            Field(name, { name = it }, label = stringResource(Res.string.profile_name))
            Field(email, { email = it }, label = stringResource(Res.string.profile_email), kind = FieldKind.Email)
            Field(headline, { headline = it }, label = stringResource(Res.string.profile_headline))
            Field(phone, { phone = it }, label = stringResource(Res.string.profile_phone))
            Field(portfolio, { portfolio = it }, label = stringResource(Res.string.profile_portfolio), kind = FieldKind.Url)
            Field(linkedin, { linkedin = it }, label = stringResource(Res.string.profile_linkedin), kind = FieldKind.Url)
            return@KiddoScreen
        }
        Column(Modifier.fillMaxWidth().padding(bottom = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(size = 76.dp)
            BasicText(state.workspace.name, style = Kiddo.type.heading.copy(color = Kiddo.colors.fg), modifier = Modifier.padding(top = 12.dp))
            if (state.profile.headline.isNotBlank()) {
                BasicText(state.profile.headline, style = Kiddo.type.body.copy(color = Kiddo.colors.muted))
            }
        }
        Group(title = stringResource(Res.string.profile_in_every)) {
            Info(stringResource(Res.string.profile_email), state.workspace.email)
            Hairline()
            Info(stringResource(Res.string.profile_headline), state.profile.headline)
            Hairline()
            Info(stringResource(Res.string.profile_phone), state.profile.phone)
            Hairline()
            Info(stringResource(Res.string.profile_portfolio), state.profile.portfolio)
            Hairline()
            Info(stringResource(Res.string.profile_linkedin), state.profile.linkedin)
        }
        Group(title = stringResource(Res.string.profile_writing), note = stringResource(Res.string.profile_writing_note)) {
            ListRow(stringResource(Res.string.profile_tone), value = stringResource(toneLabel(state.profile.tone)), onClick = onTone)
            Hairline()
            ListRow(stringResource(Res.string.profile_signature), value = stringResource(signatureLabel(state.profile.signature)), onClick = onSignature)
            Hairline()
            ListRow(stringResource(Res.string.profile_default_cv), value = state.defaultCv?.name ?: stringResource(Res.string.settings_none), onClick = onDefaultCv)
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            KiddoIcon(KiddoGlyph.Lock, null, size = 15.dp, tint = Kiddo.colors.ok)
            Box(Modifier.padding(start = 6.dp)) {
                BasicText(stringResource(Res.string.profile_stored), style = Kiddo.type.sub.copy(color = Kiddo.colors.muted))
            }
        }
    }
}

private fun toneLabel(t: Tone): StringResource = when (t) {
    Tone.Formal -> Res.string.tone_formal
    Tone.Friendly -> Res.string.tone_friendly
    Tone.Brief -> Res.string.tone_brief
}

private fun signatureLabel(s: Signature): StringResource = when (s) {
    Signature.Name -> Res.string.signature_name
    Signature.NameAndPhone -> Res.string.signature_name_phone
}

private const val STOP_AFTER = 5_000L
