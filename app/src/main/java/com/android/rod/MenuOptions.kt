package com.android.rod

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.rod.ui.theme.primary
import com.android.rod.ui.theme.secondary

@Composable
fun HomePageCategoriesCardView(title: String, icon: ImageVector, modifier: Modifier,onClick:()->Unit) {

    val density = LocalDensity.current

    Column(
        modifier = modifier
    ){

        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = 5.dp,
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .padding(10.dp)
                .clickable {
                    onClick()
                }
        ){
            Box(
                modifier = Modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center
                ){
                Icon(
                    imageVector = icon,
                    contentDescription = "" ,
                    modifier = Modifier
                        .size(100.dp),
                    tint = primary

                )
            }

        }

        Text(
            text = title ,
            fontSize = with(density){18.dp.toSp()},
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Light,
            color = secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}
