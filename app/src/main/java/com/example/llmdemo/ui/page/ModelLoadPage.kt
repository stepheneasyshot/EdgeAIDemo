package com.example.llmdemo.ui.page

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.llmdemo.appContext
import com.example.llmdemo.llm.LLManager
import com.stephen.commonhelper.utils.errorLog
import com.stephen.commonhelper.utils.infoLog
import io.shubham0204.smollm.GGUFReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ModelLoadPage(padding: PaddingValues) {

    val scope = rememberCoroutineScope()

    val partiallyResponse = remember { mutableStateOf("") }

    val internalFileName = remember { mutableStateOf("") }

    val isFileExist = remember { mutableStateOf(false) }

    val findThinkTagRegex = remember { Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            activityResult.data?.let {
                it.data?.let { uri ->
                    infoLog("Selected file uri: ${uri.path}")
                    if (checkGGUFFile(uri)) {
                        copyModelFile(uri, onComplete = { fileName ->
                            infoLog("Model file copied to files dir: $fileName")
                            internalFileName.value = fileName
                        })
                    } else {
                        errorLog("Selected file is not a GGUF file")
                    }
                }
            }
        }

    LaunchedEffect(Unit) {
        internalFileName.value = checkModelFileExist() ?: ""
        isFileExist.value = internalFileName.value.isNotEmpty()
    }

    Column(modifier = Modifier.padding(padding)) {
        Text(text = "模型加载页", fontSize = 30.sp, modifier = Modifier.padding(10.dp))

        if (isFileExist.value) {
            Text(
                text = "模型文件已存在: ${internalFileName.value}",
                fontSize = 16.sp,
                modifier = Modifier.padding(10.dp)
            )
        }

        Button(
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
            Text(text = "选取模型文件")
        }

        Button(
            onClick = {
                if (internalFileName.value.isNotEmpty())
                    scope.launch {
                        loadChat(fileName = internalFileName.value)
                    }
            },
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(1f)
                .padding(10.dp)
        ) {
            Text(text = "加载模型对话")
        }

        Button(
            onClick = {
                if (internalFileName.value.isNotEmpty()) {
                    scope.launch {
                        LLManager.getResponse(
                            query = "Hello",
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
            },
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(1f)
                .padding(10.dp)
        ) {
            Text(text = "对话Hello")
        }

        Text(text = partiallyResponse.value)
    }
}

fun copyModelFile(
    uri: Uri,
    onComplete: (String) -> Unit,
) {
    var fileName = ""
    appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        fileName = cursor.getString(nameIndex)
    }
    if (fileName.isNotEmpty()) {
        CoroutineScope(Dispatchers.IO).launch {
            appContext.contentResolver.openInputStream(uri).use { inputStream ->
                FileOutputStream(File(appContext.filesDir, fileName)).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
            }
            val ggufReader = GGUFReader()
            ggufReader.load(File(appContext.filesDir, fileName).absolutePath)
            withContext(Dispatchers.Main) {
                onComplete(fileName)
            }
        }
    } else {
        errorLog("File name is empty")
    }
}

suspend fun loadChat(fileName: String) = withContext(Dispatchers.IO) {
    val path = File(appContext.filesDir, fileName).absolutePath
    LLManager.load(absolutePath = path, onSuccess = {
        infoLog("Model loaded")
    }, onError = {
        infoLog("Model load error: $it")
    })
}

private fun checkGGUFFile(uri: Uri): Boolean {
    appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
        val ggufMagicNumberBytes = ByteArray(4)
        inputStream.read(ggufMagicNumberBytes)
        return ggufMagicNumberBytes.contentEquals(byteArrayOf(71, 71, 85, 70))
    }
    return false
}

/**
 * 查看内部目录下是否有gguf后缀文件
 */
private fun checkModelFileExist(): String? {
    val filesDir = appContext.filesDir
    val files = filesDir.listFiles()
    files?.forEach { file ->
        if (file.name.endsWith(".gguf")) {
            return file.name
        }
    }
    return null
}