package com.example.realtime_obstacle_detection.ui.screens.homepage


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.realtime_obstacle_detection.ui.theme.secondary


@Composable
fun HomePageCategoriesCardView(title: String, image: ImageVector, modifier: Modifier, onClickEvent:() -> Unit) {

    Column(
        modifier = modifier
    ){

        Card(
            shape = RoundedCornerShape(50.dp),
            elevation = 5.dp,
            modifier = Modifier
                .size(200.dp)
                .padding(10.dp)
                .clickable {
                    onClickEvent()
                }
        ){
            Icon(
                imageVector = image,
                contentDescription = "",
                tint = secondary
            )
        }

        Text(
            text = title ,
            fontSize = 18.sp ,
            fontWeight = FontWeight.Light,
            color = secondary ,
            textAlign = TextAlign.Center ,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}