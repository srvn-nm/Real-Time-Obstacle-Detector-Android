package com.example.realtime_obstacle_detection.ui.screens.homepage



import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.realtime_obstacle_detection.ui.activities.BlindDetectorActivity
import com.example.realtime_obstacle_detection.ui.activities.OnDetectionActivity
import com.example.realtime_obstacle_detection.ui.activities.WalkAroundActivity

@Composable
fun HomePageScreen(navController: NavController){

    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        LazyColumn{
            item {

                Row{

                    HomePageCategoriesCardView(
                        title = "Walking Safe" ,
                        image = Icons.Default.DirectionsWalk,
                        modifier = Modifier.weight(1f)
                    ){
                        context.startActivity(Intent(context, WalkAroundActivity::class.java))
                    }
                    HomePageCategoriesCardView(
                        title = "Visually Impaired or Blind People" ,
                        image = Icons.Default.Visibility,
                        modifier = Modifier.weight(1f)
                    ){
                        context.startActivity(Intent(context, BlindDetectorActivity::class.java))
                    }
                }

                Row{
                    HomePageCategoriesCardView(
                        title = "Bounding Box" ,
                        image =  Icons.Default.CheckBoxOutlineBlank,
                        modifier = Modifier.weight(1f)
                    ){
                            context.startActivity(Intent(context, OnDetectionActivity::class.java))
                    }
                    HomePageCategoriesCardView(
                        title = "About us" ,
                        image = Icons.Default.Info,
                        modifier = Modifier.weight(1f)
                    ){
                        navController.navigate("about_us")
                    }
                }

            }
        }
    }
}

