package com.android.rod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import com.android.rod.actionbar.AppActionBar
import com.android.rod.model.HomePageNavGraph
import com.android.rod.ui.theme.RealtimeObstaclesDetectionSystemTheme


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RealtimeObstaclesDetectionSystemTheme {
                HomePageSetUp()
            }
        }
    }
}


@Composable
@ExperimentalMaterialApi
fun HomePageSetUp() {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val navController = rememberNavController()
    androidx.compose.material.Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            AppActionBar(
                title =  stringResource(id = R.string.app_name)
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
        ) {
            HomePageNavGraph(navHostController = navController)
        }
    }
}