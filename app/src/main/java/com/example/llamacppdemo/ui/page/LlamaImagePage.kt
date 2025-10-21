package com.example.llamacppdemo.ui.page

import android.content.Intent
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.llamacppdemo.llamacpp.LLManager
import com.example.llamacppdemo.ui.component.CenterText
import com.example.llamacppdemo.ui.component.CommonButton
import com.example.llamacppdemo.ui.component.WrappedEditText
import com.example.llamacppdemo.ui.component.rememberToastState
import com.stephen.commonhelper.utils.errorLog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import com.stephen.commonhelper.utils.infoLog
import kotlinx.coroutines.launch

@Composable
fun LlamaImagePage(padding: PaddingValues) {

    val scope = rememberCoroutineScope()

    val toastState = rememberToastState()

    val partiallyResponse = remember { mutableStateOf("") }

    val chatText = remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val internalGGUFFileList = remember { mutableStateListOf<String>() }

    val checkedModel = remember { mutableStateOf("") }

    val modelLoadedState = remember { mutableStateOf(false) }

    val findThinkTagRegex = remember { Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            activityResult.data?.let {
                it.data?.let { uri ->
                    infoLog("Selected file uri: ${uri.path}")
                    if (LLManager.checkGGUFFile(uri)) {
                        LLManager.copyModelFile(uri, onComplete = { fileName ->
                            toastState.show("Model file copied to files dir: $fileName")
                            internalGGUFFileList.clear()
                            internalGGUFFileList.addAll(LLManager.checkModelFileExist())
                        })
                    } else {
                        errorLog("Selected file is not a GGUF file")
                    }
                }
            }
        }

    LaunchedEffect(Unit) {
        internalGGUFFileList.clear()
        internalGGUFFileList.addAll(LLManager.checkModelFileExist())
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                focusManager.clearFocus()
            }) {
        Text(text = "Llama.cpp 图片模型 加载页", fontSize = 30.sp, modifier = Modifier.padding(bottom = 10.dp))

        if (internalGGUFFileList.isNotEmpty()) {
            Text(
                text = "可选模型列表",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            internalGGUFFileList.forEach { fileName ->
                Text(
                    text = fileName,
                    fontSize = 16.sp,
                    color = if (modelLoadedState.value) Color.White.copy(alpha = 0.6f) else Color.White,
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth(1f)
                        .clip(RoundedCornerShape(10))
                        .background(if (checkedModel.value == fileName) Color.Green.copy(alpha = 0.4f) else Color.Transparent)
                        .clickable(
                            // 如果已加载完成，不可切换模型
                            enabled = !modelLoadedState.value,
                            onClick = {
                                checkedModel.value = fileName
                            }
                        )
                        .padding(vertical = 10.dp, horizontal = 5.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .fillMaxWidth(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 添加模型文件
            CommonButton(
                onClick = {
                    val intent =
                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            type = "application/octet-stream"
                            putExtra(
                                DocumentsContract.EXTRA_INITIAL_URI,
                                Environment
                                    .getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DOWNLOADS,
                                    ).toUri(),
                            )
                        }
                    launcher.launch(intent)
                },
                modifier = Modifier.padding(10.dp)
            ) {
                CenterText(text = "添加GGUF模型")
            }

            CommonButton(
                onClick = {
                    if (checkedModel.value.isNotEmpty()) {
                        scope.launch {
                            LLManager.loadChat(fileName = checkedModel.value) {
                                toastState.show("${checkedModel.value} 加载完毕")
                                modelLoadedState.value = true
                            }
                        }
                    } else {
                        toastState.show("请先选择模型文件")
                    }
                },
                modifier = Modifier.padding(10.dp)
            ) {
                CenterText(text = "Load加载模型")
            }
        }

        Row(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .fillMaxWidth(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WrappedEditText(
                modifier = Modifier
                    .padding(5.dp)
                    .weight(1f)
                    .focusRequester(focusRequester),
                value = chatText.value,
                onValueChange = {
                    chatText.value = it
                },
                tipText = "请输入对话内容",
                onEnterPressed = {
                    if (checkedModel.value.isNotEmpty() && chatText.value.isNotEmpty() && modelLoadedState.value) {
                        scope.launch {
                            LLManager.getResponse(
                                query = chatText.value,
                                responseTransform = {
                                    findThinkTagRegex.replace(it) { matchResult ->
                                        matchResult.groupValues[1].trim()
                                    }
                                },
                                onPartialResponseGenerated = {
                                    infoLog("Partial Response: $it")
                                },
                                onSuccess = {
                                    infoLog("Final Response: $it")
                                    partiallyResponse.value = it
                                },
                                onCancelled = {
                                    infoLog("Response cancelled")
                                },
                                onError = {
                                    infoLog("Response error: $it")
                                },
                            )
                        }
                        focusManager.clearFocus()
                    }
                })

            CommonButton(onClick = {
                focusManager.clearFocus()
                if (checkedModel.value.isNotEmpty() && chatText.value.isNotEmpty() && modelLoadedState.value) {
                    scope.launch {
                        LLManager.getResponse(
                            query = chatText.value,
                            responseTransform = {
                                findThinkTagRegex.replace(it) { matchResult ->
                                    matchResult.groupValues[1].trim()
                                }
                            },
                            onPartialResponseGenerated = {
                                infoLog("Partial Response: $it")
                            },
                            onSuccess = {
                                infoLog("Final Response: $it")
                                partiallyResponse.value = it
                            },
                            onCancelled = {
                                infoLog("Response cancelled")
                            },
                            onError = {
                                infoLog("Response error: $it")
                            },
                        )
                    }
                }
            }) {
                CenterText(text = "发送")
            }

        }

        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f)
                .fillMaxWidth(1f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp)
        ) {
            LazyColumn {
                item {
                    Text(text = partiallyResponse.value, modifier = Modifier.padding(10.dp))
                }
            }
        }
    }
}