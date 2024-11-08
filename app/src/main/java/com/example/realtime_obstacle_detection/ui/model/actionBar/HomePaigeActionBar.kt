package com.example.realtime_obstacle_detection.ui.model.actionBar


import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.realtime_obstacle_detection.ui.theme.primary
import com.example.realtime_obstacle_detection.ui.theme.secondary


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomePageActionBar(
    scope: CoroutineScope,
    scaffoldState: ScaffoldState,
    title: String
){
    TopAppBar(
        elevation = 1.dp,
        title = {
            Text(
                text = title
            )
        },
        backgroundColor = primary,
        contentColor = secondary,
        navigationIcon = {
            IconButton(
                onClick = {
                    scope.launch {
                        scaffoldState.drawerState.open()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Drawer Bottom",
                )
            }
        },

    )
}