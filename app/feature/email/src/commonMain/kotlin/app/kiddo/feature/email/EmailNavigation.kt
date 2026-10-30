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

