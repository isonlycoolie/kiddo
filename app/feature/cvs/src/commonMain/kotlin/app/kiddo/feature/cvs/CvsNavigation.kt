package app.kiddo.feature.cvs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.kiddo.core.designsystem.component.Field
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.Primary
import app.kiddo.core.designsystem.component.SheetGroup
import app.kiddo.core.designsystem.component.SheetRow
import app.kiddo.core.designsystem.component.message
import app.kiddo.core.navigation.Route
import app.kiddo.core.platform.rememberCvPicker
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_save
import app.kiddo.core.resources.cv_delete
import app.kiddo.core.resources.cv_improve_text
import app.kiddo.core.resources.cv_make_default
import app.kiddo.core.resources.cv_name
import app.kiddo.core.resources.cv_rename
import app.kiddo.core.resources.cvs_added
import app.kiddo.core.resources.generated.KiddoGlyph
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val cvsModule = module {
    viewModelOf(::CvsViewModel)
    viewModelOf(::CvViewModel)
}

fun NavGraphBuilder.cvsScreens(nav: NavController) {
    composable<Route.Cvs> {
        val viewModel: CvsViewModel = koinViewModel()
        val list by viewModel.list.collectAsStateWithLifecycle()
        val files by viewModel.files.collectAsStateWithLifecycle()
        val busy by viewModel.busy.collectAsStateWithLifecycle()
        val overlay = LocalOverlay.current
        val scope = rememberCoroutineScope()
        val pick = rememberCvPicker { result ->
            viewModel.add(result) { failure ->
                scope.launch { overlay.toast(getString(failure?.message() ?: Res.string.cvs_added)) }
            }
        }
        CvsScreen(list, files, busy, { nav.popBackStack() }, pick, { nav.navigate(Route.Cv(it)) }, viewModel::loadFile)
    }
    composable<Route.Cv> { entry ->
        val id = entry.toRoute<Route.Cv>().id
        val viewModel: CvViewModel = koinViewModel()
        LaunchedEffect(id) { viewModel.open(id) }
        val cv by viewModel.cv.collectAsStateWithLifecycle()
        val bytes by viewModel.file.collectAsStateWithLifecycle()
        val overlay = LocalOverlay.current
        val improve = stringResource(Res.string.cv_improve_text, cv?.name.orEmpty())
        val renameTitle = stringResource(Res.string.cv_rename)
        CvScreen(
            cv,
            bytes,
            onBack = { nav.popBackStack() },
            onMore = {
                overlay.sheet {
                    SheetGroup {
                        if (cv?.isDefault == false) CvSheetRow(Res.string.cv_make_default, KiddoGlyph.Check) { viewModel.makeDefault().also { overlay.close() } }
                        CvSheetRow(Res.string.cv_rename, KiddoGlyph.NewChat) {
                            overlay.sheet(renameTitle) { RenameForm(cv?.name.orEmpty()) { name -> viewModel.rename(name).also { overlay.close() } } }
                        }
                    }
                    SheetGroup {
                        CvSheetRow(Res.string.cv_delete, KiddoGlyph.Trash, danger = true) {
                            overlay.close()
                            viewModel.delete { nav.popBackStack() }
                        }
                    }
                }
            },
            onImprove = { nav.navigate(Route.Home(draft = improve)) { popUpTo<Route.Home> { inclusive = true } } },
        )
    }
}

@Composable
private fun CvSheetRow(label: org.jetbrains.compose.resources.StringResource, glyph: KiddoGlyph, danger: Boolean = false, onClick: () -> Unit) {
    SheetRow(stringResource(label), onClick, glyph = glyph, danger = danger)
}

@Composable
private fun RenameForm(current: String, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    Field(name, { name = it }, label = stringResource(Res.string.cv_name))
    Primary(stringResource(Res.string.action_save), { onSave(name) }, enabled = name.isNotBlank())
}
