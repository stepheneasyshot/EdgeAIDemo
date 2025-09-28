package com.example.edgeaidemo.ui.page

import android.content.Intent
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.edgeaidemo.llamacpp.LLManager
import com.example.edgeaidemo.ui.component.CenterText
import com.example.edgeaidemo.ui.component.CommonButton
import com.example.edgeaidemo.ui.component.WrappedEditText
import com.example.edgeaidemo.ui.component.rememberToastState
import com.stephen.commonhelper.utils.errorLog
import com.stephen.commonhelper.utils.infoLog
import kotlinx.coroutines.launch

@Composable
fun ModelLoadPage(padding: PaddingValues) {

    val scope = rememberCoroutineScope()

    val toastState = rememberToastState()

    val partiallyResponse = remember { mutableStateOf("") }

    val chatText = remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val internalFileName = remember { mutableStateOf("") }

    val isFileExist = remember { mutableStateOf(false) }

    val findThinkTagRegex = remember { Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            activityResult.data?.let {
                it.data?.let { uri ->
                    infoLog("Selected file uri: ${uri.path}")
                    if (LLManager.checkGGUFFile(uri)) {
                        LLManager.copyModelFile(uri, onComplete = { fileName ->
                            toastState.show("Model file copied to files dir: $fileName")
                            internalFileName.value = fileName
                            isFileExist.value = internalFileName.value.isNotEmpty()
                        })
                    } else {
                        errorLog("Selected file is not a GGUF file")
                    }
                }
            }
        }

    LaunchedEffect(Unit) {
        internalFileName.value = LLManager.checkModelFileExist() ?: ""
        isFileExist.value = internalFileName.value.isNotEmpty()
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
        Text(text = "模型加载页", fontSize = 30.sp, modifier = Modifier.padding(10.dp))

        if (isFileExist.value) {
            Text(
                text = "模型文件已存在: ${internalFileName.value}",
                fontSize = 16.sp,
                modifier = Modifier.padding(10.dp)
            )
        } else {
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
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(1f)
                    .padding(10.dp)
            ) {
                CenterText(text = "选取模型gguf文件")
            }
        }

        CommonButton(
            onClick = {
                if (internalFileName.value.isNotEmpty())
                    scope.launch {
                        LLManager.loadChat(fileName = internalFileName.value) {
                            toastState.show("$internalFileName 加载完毕")
                        }
                    }
            },
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(1f)
                .padding(10.dp)
        ) {
            CenterText(text = "Load加载模型")
        }

        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {

            WrappedEditText(
                modifier = Modifier
                    .padding(10.dp)
                    .weight(1f)
                    .focusRequester(focusRequester),
                value = chatText.value,
                onValueChange = {
                    chatText.value = it
                },
                tipText = "请输入对话内容",
                onEnterPressed = {
                    if (internalFileName.value.isNotEmpty() && chatText.value.isNotEmpty()) {
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
                if (internalFileName.value.isNotEmpty() && chatText.value.isNotEmpty()) {
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

        Text(text = partiallyResponse.value, modifier = Modifier.padding(10.dp))
    }
}