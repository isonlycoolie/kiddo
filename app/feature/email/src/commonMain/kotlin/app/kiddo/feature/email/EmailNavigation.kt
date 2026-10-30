package app.kiddo.feature.email

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.SheetGroup
import app.kiddo.core.designsystem.component.SheetRow
import app.kiddo.core.designsystem.component.message
import app.kiddo.core.model.MailSecurity
import app.kiddo.core.navigation.Route
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.account_test_sent
import app.kiddo.core.resources.smtp_security
import app.kiddo.core.resources.test_body
import app.kiddo.core.resources.test_subject
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val emailModule = module { viewModelOf(::EmailViewModel) }

// After connecting, the account page replaces the form, so Back returns to the Email list.
private fun NavController.toAccount() = navigate(Route.EmailAccount) { popUpTo<Route.Email>() }

fun NavGraphBuilder.emailScreens(nav: NavController) {
    composable<Route.Email> {
        val viewModel: EmailViewModel = koinViewModel()
        val account by viewModel.account.collectAsStateWithLifecycle()
        EmailScreen(
            account,
            EmailActions(
                onBack = { nav.popBackStack() },
                onAccount = { nav.navigate(Route.EmailAccount) },
                onGmail = { nav.navigate(Route.GmailGuide) },
                onGoogle = { nav.navigate(Route.GoogleSignIn) },
                onSmtp = { nav.navigate(Route.Smtp) },
                onSendly = { nav.navigate(Route.Sendly) },
            ),
        )
    }
    composable<Route.GmailGuide> {
        val uri = LocalUriHandler.current
        GmailGuideScreen({ nav.popBackStack() }, { uri.openUri(it) }, { nav.navigate(Route.GmailPassword) })
    }
    composable<Route.GoogleSignIn> {
        GoogleSignInScreen({ nav.popBackStack() }, { nav.navigate(Route.GmailGuide) { popUpTo<Route.Email>() } })
    }
    composable<Route.GmailPassword> { entry ->
        val viewModel = formViewModel(entry)
        val form by viewModel.form.collectAsStateWithLifecycle()
        GmailPasswordScreen(form, FormActions({ nav.popBackStack() }, viewModel::edit) { viewModel.connectGmail { nav.toAccount() } })
    }
    composable<Route.Smtp> { entry ->
        val viewModel = formViewModel(entry)
        val form by viewModel.form.collectAsStateWithLifecycle()
        val overlay = LocalOverlay.current
        val title = stringResource(Res.string.smtp_security)
        SmtpScreen(
            form,
            onPickSecurity = {
                overlay.sheet(title) {
                    SheetGroup {
                        MailSecurity.entries.forEach { s ->
                            SheetRow(securityLabel(s), {
                                viewModel.edit { it.copy(security = s, smtpPort = if (s == MailSecurity.SslTls) "465" else "587") }
                                overlay.close()
                            }, picked = s == form.security)
                        }
                    }
                }
            },
            actions = FormActions({ nav.popBackStack() }, viewModel::edit) { viewModel.connectSmtp { nav.toAccount() } },
        )
    }
    composable<Route.Sendly> { entry ->
        val viewModel = formViewModel(entry)
        val form by viewModel.form.collectAsStateWithLifecycle()
        val uri = LocalUriHandler.current
        SendlyScreen(form, { uri.openUri(SENDLY_URL) }, FormActions({ nav.popBackStack() }, viewModel::edit) { viewModel.connectSendly { nav.toAccount() } })
    }
    composable<Route.EmailAccount> {
        val viewModel: EmailViewModel = koinViewModel()
        val account by viewModel.account.collectAsStateWithLifecycle()
        val check by viewModel.check.collectAsStateWithLifecycle()
        val overlay = LocalOverlay.current
        val scope = rememberCoroutineScope()
        var testing by remember { mutableStateOf(false) }
        val subject = stringResource(Res.string.test_subject)
        val body = stringResource(Res.string.test_body)
        EmailAccountScreen(
            account,
            check,
            testing,
            AccountActions(
                onBack = { nav.popBackStack() },
                onTest = {
                    testing = true
                    viewModel.sendTest(subject, body) { failure ->
                        testing = false
                        scope.launch { overlay.toast(getString(failure?.message() ?: Res.string.account_test_sent)) }
                    }
                },
                onDisconnect = { viewModel.disconnect { nav.popBackStack() } },
            ),
        )
    }
}

@androidx.compose.runtime.Composable
private fun formViewModel(entry: NavBackStackEntry): EmailViewModel {
    val viewModel: EmailViewModel = koinViewModel(viewModelStoreOwner = entry)
    LaunchedEffect(Unit) { viewModel.prefill() }
    return viewModel
}
