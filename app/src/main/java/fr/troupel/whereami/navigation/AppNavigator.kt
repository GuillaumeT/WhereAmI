package fr.troupel.whereami.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.troupel.whereami.ui.menu.MenuScreen
import fr.troupel.whereami.ui.options.OptionsScreen

@Composable
fun AppNavigator() {
    val navController = rememberNavController()

    NavHost(navController=navController, startDestination = Screen.Menu.route) {
        composable(Screen.Menu.route) {
            MenuScreen { navController.navigate(it) }
        }
        composable(Screen.Options.route) {
            OptionsScreen { navController.popBackStack() }
        }
    }
}