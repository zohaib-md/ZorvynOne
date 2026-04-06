package com.project.zorvynone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.zorvynone.model.AppDatabase
import com.project.zorvynone.ui.screens.AddTransactionScreen
import com.project.zorvynone.ui.screens.HomeScreen
import com.project.zorvynone.ui.screens.InsightsScreen
import com.project.zorvynone.ui.screens.ScoreScreen
import com.project.zorvynone.ui.screens.TransactionsScreen
import com.project.zorvynone.ui.theme.ZorvynOneTheme
import com.project.zorvynone.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(db.transactionDao()) as T
            }
        }

        setContent {
            ZorvynOneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: HomeViewModel = viewModel(factory = factory)
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: HomeViewModel) {
    val navController = rememberNavController()

    // THE ULTIMATE FIX: Official Google Standard for Bottom Nav
    val navTo = { route: String ->
        val currentRoute = navController.currentBackStackEntry?.destination?.route

        // Only navigate if we aren't already on this screen!
        if (currentRoute != route) {
            navController.navigate(route) {
                // Dynamically find the start destination instead of hardcoding "home"
                navController.graph.startDestinationRoute?.let { startRoute ->
                    popUpTo(startRoute) {
                        saveState = true
                    }
                }
                // Avoid multiple copies of the same screen
                launchSingleTop = true
                // Restore state if we previously visited this tab
                restoreState = true
            }
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                // The big card acts like a standard forward navigation without saving state
                onScoreClick = { navController.navigate("score") },
                // Bottom navigation uses our special navTo helper
                onAddClick = { navTo("add_transaction") },
                onTxnsClick = { navTo("transactions") },
                onInsightsClick = { navTo("insights") },
                onScoreNavClick = { navTo("score") }
            )
        }

        composable("transactions") {
            TransactionsScreen(
                viewModel = viewModel,
                onNavigateHome = { navTo("home") },
                onNavigateAdd = { navTo("add_transaction") }
            )
        }

        composable("insights") {
            InsightsScreen(
                viewModel = viewModel,
                onNavigateHome = { navTo("home") },
                onNavigateTxns = { navTo("transactions") },
                onNavigateAdd = { navTo("add_transaction") }
            )
        }

        composable("score") {
            ScoreScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }, // Custom back button
                onNavigateHome = { navTo("home") },
                onNavigateTxns = { navTo("transactions") },
                onNavigateAdd = { navTo("add_transaction") },
                onNavigateInsights = { navTo("insights") }
            )
        }

        composable("add_transaction") {
            AddTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}