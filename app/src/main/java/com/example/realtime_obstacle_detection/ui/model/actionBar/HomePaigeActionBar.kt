package com.example.realtime_obstacle_detection.ui.model.actionBar

import androidx.compose.material.icons.Icons
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.filled.Computer
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.realtime_obstacle_detection.ui.theme.primary
import com.example.realtime_obstacle_detection.ui.theme.secondary


@Composable
fun HomePageActionBar(
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

                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Computer,
                    contentDescription = "Drawer Bottom",
                )
            }
        },

    )
}