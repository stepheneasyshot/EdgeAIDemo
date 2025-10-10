package com.example.edgeaidemo.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NavMainPage(list: List<String>, paddingValues: PaddingValues, onClickItem: (String) -> Unit) {
    LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize(1f),
        horizontalAlignment = Alignment.CenterHorizontally) {

        item {
            Text("Edge AI Demo",
                fontSize = 24.sp,
                modifier = Modifier.padding(vertical = 20.dp))
        }

        items(list) {
            Text(it,
                color = Color.Black,
                modifier = Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(10))
                .background(color = Color(0xFFF0F0F0))
                .padding(16.dp)
                .clickable {
                    onClickItem(it)
                })
        }
    }
}