package com.apsmkimo.zenwoodsudoku.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.apsmkimo.zenwoodsudoku.domain.repository.SudokuRepository
import com.apsmkimo.zenwoodsudoku.ui.bootstrap.BootstrapUiState
import com.apsmkimo.zenwoodsudoku.ui.bootstrap.BootstrapViewModel
import com.apsmkimo.zenwoodsudoku.ui.bootstrap.ImportScreen
import com.apsmkimo.zenwoodsudoku.ui.game.GameScreen
import com.apsmkimo.zenwoodsudoku.ui.game.GameViewModel
import com.apsmkimo.zenwoodsudoku.ui.home.HomeScreen
import com.apsmkimo.zenwoodsudoku.ui.home.HomeViewModel

@Composable
fun ZenWoodRoot(
    bootstrapViewModel: BootstrapViewModel,
    repository: SudokuRepository,
) {
    val boot by bootstrapViewModel.state.collectAsStateWithLifecycle()
    if (boot.phase != BootstrapUiState.Phase.Ready) {
        ImportScreen(state = boot, onRetry = bootstrapViewModel::start)
        return
    }
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(repository),
            )
            HomeScreen(viewModel = homeViewModel) { levelId ->
                navController.navigate("game/$levelId")
            }
        }
        composable(
            route = "game/{levelId}",
            arguments = listOf(navArgument("levelId") { type = NavType.IntType }),
        ) { entry ->
            val levelId = entry.arguments?.getInt("levelId") ?: return@composable
            val gameViewModel: GameViewModel = viewModel(
                factory = GameViewModel.factory(repository, levelId),
            )
            GameScreen(viewModel = gameViewModel, onBack = { navController.popBackStack() })
        }
    }
}
