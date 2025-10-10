package com.example.edgeaidemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.edgeaidemo.ui.page.AiCoreChatDemo
import com.example.edgeaidemo.ui.page.LiteRTDemoPage
import com.example.edgeaidemo.ui.page.ModelLoadPage
import com.example.edgeaidemo.ui.page.NavMainPage
import com.example.edgeaidemo.ui.theme.LLMDemoTheme
import com.stephen.commonhelper.utils.infoLog
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        infoLog("onCreate")
        enableEdgeToEdge()
        setContent {
            LLMDemoTheme {
                Surface(
                    Modifier
                        .fillMaxSize(1f)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val navController = rememberNavController()

                    val itemList = listOf(
                        MainPageItem.LLAMACPP,
                        MainPageItem.AICORE,
                        MainPageItem.LITERT,
                    )

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        // 右侧内容区
                        Box(modifier = Modifier.fillMaxSize(1f)) {
                            NavHost(navController, startDestination = MainPageItem.MAIN) {
                                composable(MainPageItem.MAIN) {
                                    NavMainPage(
                                        list = itemList,
                                        paddingValues = innerPadding,
                                        onClickItem = {
                                            navController.navigate(it)
                                        })
                                }
                                composable(MainPageItem.LLAMACPP) {
                                    ModelLoadPage(innerPadding)
                                }
                                composable(MainPageItem.AICORE) {
                                    AiCoreChatDemo(innerPadding)
                                }
                                composable(MainPageItem.LITERT) {
                                    LiteRTDemoPage(innerPadding)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        infoLog("onDestroy")
    }
}

@Serializable
object MainPageItem {
    const val MAIN ="MAIN"
    const val LLAMACPP ="LLAMACPP"
    const val AICORE ="AICORE"
    const val LITERT ="LITERT"
}
