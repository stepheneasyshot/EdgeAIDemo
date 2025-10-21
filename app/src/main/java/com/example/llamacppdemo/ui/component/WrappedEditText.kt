package com.example.llamacppdemo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.example.llamacppdemo.ui.theme.defaultText

@Composable
fun WrappedEditText(
    value: String,
    onValueChange: (String) -> Unit,
    tipText: String,
    modifier: Modifier = Modifier,
    onEnterPressed: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }

    var ctrlPressed by remember { mutableStateOf(false) }

    var altPressed by remember { mutableStateOf(false) }

    TextField(
        value = value,
        textStyle = defaultText,
        colors = TextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
            cursorColor = MaterialTheme.colorScheme.onPrimary,
            focusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            focusedContainerColor = MaterialTheme.colorScheme.background,
        ),
        label = { Text(tipText, color = MaterialTheme.colorScheme.onSecondary) },
        onValueChange = {
            // 如果此时使用了ctrl或者alt键，那么就不做处理
            // 否则就处理，丢弃掉最后一个换行符
            onValueChange(if (!ctrlPressed && !altPressed) it.processText() else it)
        },
        modifier = modifier
            .widthIn(max = 200.dp, min = 100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(2.dp, MaterialTheme.colorScheme.onSecondary, RoundedCornerShape(20.dp))
            .focusRequester(focusRequester)
            .onKeyEvent {
                // 只有单独按下enter键才触发，其余组合键只换行
                if (it.isCtrlPressed) {
                    ctrlPressed = true
                    return@onKeyEvent false
                } else {
                    ctrlPressed = false
                }
                if (it.isAltPressed) {
                    altPressed = true
                    return@onKeyEvent false
                } else {
                    altPressed = false
                }
                if (it.key == Key.Enter) {
                    onEnterPressed()
                    return@onKeyEvent true
                }
                false
            },
    )
}

/**
 * 用来兜底TextField的bug，暂时没有找到更好的解决方案
 * 手动丢弃掉最后一个换行符
 */
private fun String.processText(): String {
    return if (this.endsWith("\n")) {
        // 如果是单一个换行符，直接置空
        // 如果非单换行符，就丢弃最后一个字符
        if (this.length == 1) ""
        else this.dropLast(1)
    } else this
}