package com.example.realtime_obstacle_detection.ui.model.screens


sealed class HomeScreens(val route:String){

    object MainMenu : HomeScreens(route = "main_menu")
    object AboutMenu : HomeScreens(route = "about_menu")

}
