package app.kiddo.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import app.kiddo.core.designsystem.KiddoMotion
import kotlinx.serialization.Serializable

// One route per screen. Features meet only through these: a feature registers its own screens and navigates to
// other features' routes, never to their code.
sealed interface Route {
    @Serializable data object Splash : Route

    @Serializable data object Welcome : Route

    @Serializable data object Workspace : Route

    @Serializable data object LockSetup : Route

    @Serializable data class Home(val chatId: String? = null, val draft: String? = null, val improveCv: String? = null) : Route

    @Serializable data class Draft(val messageId: String) : Route

    @Serializable data class Browser(val url: String) : Route

    @Serializable data object Applications : Route

    @Serializable data class Application(val id: String) : Route

    @Serializable data object Updates : Route

    @Serializable data class FollowUp(val applicationId: String) : Route

    @Serializable data object FollowUps : Route

    @Serializable data object Backup : Route

    @Serializable data object History : Route

    @Serializable data object Cvs : Route

    @Serializable data class Cv(val id: String) : Route

    @Serializable data object Profile : Route

    @Serializable data object Settings : Route

    @Serializable data object Security : Route

    @Serializable data object About : Route

    @Serializable data object Ai : Route

    @Serializable data class AiAdd(val accountId: String? = null) : Route

    @Serializable data object Email : Route

    @Serializable data object GmailGuide : Route

    @Serializable data object GmailPassword : Route

    @Serializable data object EmailAccount : Route

    @Serializable data object Smtp : Route

    @Serializable data object Sendly : Route

    @Serializable data object GoogleSignIn : Route
}

// Leaves the start flow for good: nothing before the destination stays on the back stack.
fun NavController.startOver(route: Route) = navigate(route) { clearAll(this@startOver) }

private fun NavOptionsBuilder.clearAll(nav: NavController) {
    popUpTo(nav.graph.id) { inclusive = true }
    launchSingleTop = true
}

private typealias Enter = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias Exit = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

