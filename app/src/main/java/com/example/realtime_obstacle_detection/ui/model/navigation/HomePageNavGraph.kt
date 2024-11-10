package com.example.realtime_obstacle_detection.ui.model.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.realtime_obstacle_detection.ui.model.screens.HomeScreens
import com.example.realtime_obstacle_detection.ui.screens.about.AboutUsPageScreen
import com.example.realtime_obstacle_detection.ui.screens.homepage.HomePageScreen


@ExperimentalMaterialApi
@Composable
fun HomePageNavGraph (navHostController: NavHostController){

    NavHost(
        navController = navHostController,
        startDestination = HomeScreens.MainMenu.route
    ) {
        composable(route = HomeScreens.MainMenu.route) {
            HomePageScreen(navController =  navHostController)
        }

        composable(route = HomeScreens.AboutUs.route) {
            AboutUsPageScreen()
        }

    }
}


