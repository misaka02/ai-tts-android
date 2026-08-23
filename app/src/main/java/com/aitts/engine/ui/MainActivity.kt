package com.aitts.engine.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.ui.screens.HomeScreen
import com.aitts.engine.ui.screens.ProviderConfigScreen
import com.aitts.engine.ui.screens.ProviderListScreen
import com.aitts.engine.ui.screens.RulesScreen
import com.aitts.engine.ui.screens.SettingsScreen
import com.aitts.engine.ui.screens.TestBenchScreen
import com.aitts.engine.ui.theme.AiTtsEngineTheme

class MainActivity : ComponentActivity() {

    private lateinit var configDataStore: ConfigDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configDataStore = ConfigDataStore.getInstance(this)

        // 启动时自动申请必要权限
        PermissionManager.requestBasicPermissions(this)

        setContent {
            val settings by configDataStore.settingsFlow.collectAsState()

            AiTtsEngineTheme(
                themeMode = settings.appThemeMode,
                themePalette = settings.appThemePalette
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppNavHost(configDataStore)
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Providers : Screen("providers", "模型", Icons.Default.RecordVoiceOver)
    object Rules : Screen("rules", "规则", Icons.Default.Spellcheck)
    object TestBench : Screen("testbench", "沙盒", Icons.Default.GraphicEq)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

@Composable
fun MainAppNavHost(configDataStore: ConfigDataStore) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Providers,
        Screen.Rules,
        Screen.TestBench,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            if (bottomNavItems.any { it.route == currentRoute }) {
                val primaryColor = MaterialTheme.colorScheme.primary
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = {
                                Text(
                                    screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = primaryColor,
                                selectedTextColor = primaryColor,
                                indicatorColor = primaryColor.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    configDataStore = configDataStore,
                    onNavigateToEditProvider = { id ->
                        navController.navigate("provider_edit/$id")
                    },
                    onNavigateToTestBench = {
                        navController.navigate(Screen.TestBench.route)
                    }
                )
            }
            composable(Screen.Providers.route) {
                ProviderListScreen(
                    configDataStore = configDataStore,
                    onNavigateToEditProvider = { id ->
                        navController.navigate("provider_edit/$id")
                    }
                )
            }
            composable(
                route = "provider_edit/{providerId}",
                arguments = listOf(navArgument("providerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
                ProviderConfigScreen(
                    providerId = providerId,
                    configDataStore = configDataStore,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Rules.route) {
                RulesScreen(configDataStore = configDataStore)
            }
            composable(Screen.TestBench.route) {
                TestBenchScreen(
                    configDataStore = configDataStore,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(configDataStore = configDataStore)
            }
        }
    }
}
