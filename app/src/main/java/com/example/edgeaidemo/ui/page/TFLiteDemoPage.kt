package com.example.edgeaidemo.ui.page

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaidemo.litert.LiteRTLoadManager
import com.example.edgeaidemo.ui.component.rememberToastState
import androidx.compose.runtime.LaunchedEffect

@Composable
fun TFLiteDemoPage(paddingValues: PaddingValues) {

    val scope = rememberCoroutineScope()

    val toastState = rememberToastState()

    LaunchedEffect(Unit) {
        LiteRTLoadManager.init()
    }

    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                "TFLite Demo",
                fontSize = 24.sp,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        }
    }
}