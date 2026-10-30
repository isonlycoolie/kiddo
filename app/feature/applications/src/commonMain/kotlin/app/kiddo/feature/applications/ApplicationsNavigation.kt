package app.kiddo.feature.applications

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.SheetGroup
import app.kiddo.core.designsystem.component.SheetRow
import app.kiddo.core.designsystem.component.message
import app.kiddo.core.model.AppStatus
import app.kiddo.core.model.NoticeKind
import app.kiddo.core.navigation.Route
import app.kiddo.core.platform.shortDate
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.app_delete
import app.kiddo.core.resources.follow_body
import app.kiddo.core.resources.follow_sent
import app.kiddo.core.resources.follow_subject
import app.kiddo.core.resources.followups_after
import app.kiddo.core.resources.followups_first
import app.kiddo.core.resources.followups_max
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.status_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val applicationsModule = module {
    singleOf(::InboxWatcher)
    factoryOf(::FollowUpSender)
    viewModelOf(::ApplicationsViewModel)
}

