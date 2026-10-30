package app.kiddo.feature.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.Overlay
import app.kiddo.core.designsystem.component.ProviderLogo
import app.kiddo.core.designsystem.component.SheetGroup
import app.kiddo.core.designsystem.component.SheetRow
import app.kiddo.core.designsystem.component.accountSheet
import app.kiddo.core.model.AiProvider
import app.kiddo.core.navigation.Route
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.ai_choose_model
import app.kiddo.core.resources.ai_ollama_sub
import app.kiddo.core.resources.ai_openrouter_sub
import app.kiddo.core.resources.ai_other_sub
import app.kiddo.core.resources.ai_provider
import app.kiddo.core.resources.ai_use_chat
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val modelsModule = module {
    viewModelOf(::AiViewModel)
    viewModelOf(::AddViewModel)
}

fun NavGraphBuilder.modelsScreens(nav: NavController) {
    composable<Route.Ai> {
        val viewModel: AiViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val overlay = LocalOverlay.current
        val chatTitle = stringResource(Res.string.ai_use_chat)
        AiScreen(
            state,
            AiActions(
                onBack = { nav.popBackStack() },
                onAdd = { nav.navigate(Route.AiAdd()) },
                onOpen = { nav.navigate(Route.AiAdd(it)) },
                onPickChat = { overlay.accountSheet(chatTitle, state.accounts, state.chat?.id, viewModel::useForChat) },
            ),
        )
    }
    composable<Route.AiAdd> { entry ->
        val viewModel: AddViewModel = koinViewModel()
        val accountId = entry.toRoute<Route.AiAdd>().accountId
        LaunchedEffect(accountId) { viewModel.load(accountId) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val overlay = LocalOverlay.current
        val providerTitle = stringResource(Res.string.ai_provider)
        val modelTitle = stringResource(Res.string.ai_choose_model)
        AddScreen(
            state,
            AddActions(
                onBack = { nav.popBackStack() },
                onPickProvider = { overlay.providerSheet(providerTitle, state.provider, viewModel::provider) },
                onKey = viewModel::key,
                onBaseUrl = viewModel::baseUrl,
                onModel = viewModel::model,
                onPickModel = {
                    if (state.models.isEmpty()) viewModel.refreshModels()
                    else overlay.modelSheet(modelTitle, state.models, state.model, viewModel::model)
                },
                onSave = { viewModel.testAndSave { nav.popBackStack() } },
                onRemove = { viewModel.remove { nav.popBackStack() } },
            ),
        )
    }
}

private fun Overlay.providerSheet(title: String, current: AiProvider, onPick: (AiProvider) -> Unit) = sheet(title) {
    SheetGroup {
        AiProvider.entries.forEach { p ->
            SheetRow(p.label, { onPick(p).also { close() } }, lead = { ProviderLogo(p, size = 34.dp) }, sub = providerSub(p), picked = p == current)
        }
    }
}

@Composable
private fun providerSub(p: AiProvider): String? = when (p) {
    AiProvider.OpenRouter -> stringResource(Res.string.ai_openrouter_sub)
    AiProvider.Ollama -> stringResource(Res.string.ai_ollama_sub)
    AiProvider.Other -> stringResource(Res.string.ai_other_sub)
    else -> null
}

private fun Overlay.modelSheet(title: String, models: List<String>, current: String, onPick: (String) -> Unit) = sheet(title) {
    SheetGroup { models.forEach { m -> SheetRow(m, { onPick(m).also { close() } }, picked = m == current) } }
}
