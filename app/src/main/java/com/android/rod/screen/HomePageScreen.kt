package com.android.rod.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Output
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.rod.HomePageCategoriesCardView
import com.android.rod.R
import com.android.rod.ui.theme.tertiary

@Composable
fun HomePageScreen() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = tertiary),
        contentAlignment = Alignment.Center
    ){
        LazyColumn{
            item {

                Row{

                    HomePageCategoriesCardView(
                        title = stringResource(id = R.string.run_inside) ,
                        icon = Icons.AutoMirrored.Filled.Input,
                        modifier = Modifier.weight(1f),
                    ){

                    }

                    HomePageCategoriesCardView(
                        title = stringResource(id = R.string.floating_run) ,
                        icon = Icons.Filled.GridOn,
                        modifier = Modifier.weight(1f),
                    ){

                    }
                }

                Row{

                    HomePageCategoriesCardView(
                        title = stringResource(id = R.string.background_run) ,
                        icon = Icons.Filled.Output,
                        modifier = Modifier.weight(1f),
                    ){

                    }

                    HomePageCategoriesCardView(
                        title = stringResource(id = R.string.about) ,
                        icon = Icons.Filled.Info,
                        modifier = Modifier.weight(1f),
                    ){

                    }
                }
            }
        }
    }
}