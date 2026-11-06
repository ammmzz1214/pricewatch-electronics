package com.pricewatch.electronics.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pricewatch.electronics.presentation.history.HistoryScreen
import com.pricewatch.electronics.presentation.products.MyProductsScreen
import com.pricewatch.electronics.presentation.products.ProductDetailScreen
import com.pricewatch.electronics.presentation.search.SearchScreen
import com.pricewatch.electronics.presentation.settings.SettingsScreen
import com.pricewatch.electronics.presentation.sites.SiteDetailScreen
import com.pricewatch.electronics.presentation.sites.SitesScreen

/**
 * Search is the home screen: one window where the user types a product
 * name, sets specs and a price range, and every built-in shop is searched.
 * There is no Add Product screen and no URL field anywhere in the app —
 * tracked products come from search results, with the shop's URL saved
 * behind the scenes.
 */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Search : Screen("search", "Search", Icons.Filled.Search)
    data object Products : Screen("products", "My Products", Icons.Filled.List)
    data object Sites : Screen("sites", "Sites", Icons.Filled.Store)
    data object History : Screen("history", "History", Icons.Filled.History)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    data object ProductDetail : Screen("product_detail/{productId}", "Product Detail", Icons.Filled.List)
    data object SiteDetail : Screen("site_detail/{siteId}", "Site Detail", Icons.Filled.Store)

    companion object {
        val bottomNavItems = listOf(Search, Products, Sites, History, Settings)
        fun productDetailRoute(productId: Long) = "product_detail/$productId"
        fun siteDetailRoute(siteId: Long) = "site_detail/$siteId"
    }
}

@Composable
fun PriceWatchNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = Screen.bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Screen.bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
            startDestination = Screen.Search.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Search.route) {
                SearchScreen()
            }
            composable(Screen.Products.route) {
                MyProductsScreen(
                    onNavigateToProduct = { id -> navController.navigate(Screen.productDetailRoute(id)) },
                    onNavigateToAddProduct = { navController.navigate(Screen.Search.route) }
                )
            }
            composable(Screen.Sites.route) {
                SitesScreen(
                    onNavigateToSite = { id -> navController.navigate(Screen.siteDetailRoute(id)) }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateToProduct = { id -> navController.navigate(Screen.productDetailRoute(id)) }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") {
                    type = NavType.LongType
                })
            ) { entry ->
                val productId = entry.arguments?.getLong("productId")
                    ?: throw IllegalArgumentException("productId argument is required")
                ProductDetailScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.SiteDetail.route,
                arguments = listOf(navArgument("siteId") {
                    type = NavType.LongType
                })
            ) { entry ->
                val siteId = entry.arguments?.getLong("siteId")
                    ?: throw IllegalArgumentException("siteId argument is required")
                SiteDetailScreen(
                    siteId = siteId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
