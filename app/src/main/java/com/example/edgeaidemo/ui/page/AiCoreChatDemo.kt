package com.example.edgeaidemo.ui.page

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.edgeaidemo.appContext
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.DownloadConfig
import com.google.ai.edge.aicore.DownloadCallback
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.DisposableEffect

@Composable
fun AiCoreChatDemo(paddingValues: PaddingValues) {

    val input = remember {
        mutableStateOf(
            "What is Quantum Physics?"
        )
    }

    val outputState = aiCoreOutput.collectAsState()

    LaunchedEffect(Unit) {
        startChat(input.value)
    }

    LazyColumn(modifier = Modifier.padding(paddingValues)) {
        item {
            Text(text = "Input: ${input.value}")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Output: ${outputState.value}")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            aiCoreOutput.value = ""
            closeChatResponse()
        }
    }
}

val aiCoreOutput = MutableStateFlow("")

@SuppressLint("StaticFieldLeak")
val generationConfig = generationConfig {
    context = appContext
    temperature = 0.2f
    topK = 16
    maxOutputTokens = 256
}

val downloadCallback = object : DownloadCallback {
    override fun onDownloadProgress(totalBytesDownloaded: Long) {
        super.onDownloadProgress(totalBytesDownloaded)
        println("Download progress: $totalBytesDownloaded")
    }

    override fun onDownloadCompleted() {
        super.onDownloadCompleted()
        println("Download completed")
    }
}

val downloadConfig = DownloadConfig(downloadCallback)
val generativeModel = GenerativeModel(
    generationConfig = generationConfig,
    downloadConfig = downloadConfig
)

suspend fun startChat(input: String) {
    runCatching {
        val response = generativeModel.generateContent(input)
        print(response.text)
        aiCoreOutput.value = response.text.toString()
    }.onFailure { e ->
        e.printStackTrace()
    }
}

fun closeChatResponse() {
    println("Closing chat response")
    generativeModel.close()
}