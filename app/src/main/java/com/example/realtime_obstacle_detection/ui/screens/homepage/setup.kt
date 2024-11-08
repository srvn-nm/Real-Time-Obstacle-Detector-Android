package com.example.realtime_obstacle_detection.ui.screens.homepage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.realtime_obstacle_detection.ui.theme.primary

import androidx.navigation.compose.rememberNavController
import com.example.realtime_obstacle_detection.ui.model.actionBar.HomePageActionBar
import com.example.realtime_obstacle_detection.ui.model.navigation.HomePageNavGraph


@ExperimentalMaterialApi
@Composable
fun HomePageSetUp() {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            HomePageActionBar(
                scope = scope,
                scaffoldState = scaffoldState,
                title = "Real-time Obstacle Detector"
            )
        },
        backgroundColor = primary,

    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
        ) {
            HomePageNavGraph(navHostController = navController)
        }
    }
}