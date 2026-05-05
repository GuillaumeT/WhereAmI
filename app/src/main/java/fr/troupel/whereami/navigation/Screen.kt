package fr.troupel.whereami.navigation

sealed class Screen(val route: String) {
    object Menu : Screen("menu")
    object Game : Screen("game")
    object Options : Screen("options")
}