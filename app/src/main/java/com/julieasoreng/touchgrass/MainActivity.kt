package com.julieasoreng.touchgrass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.julieasoreng.touchgrass.ui.navigation.BloomNavHost
import com.julieasoreng.touchgrass.ui.theme.BloomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BloomTheme {
                BloomNavHost()
            }
        }
    }
}
