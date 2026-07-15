package com.julieasoreng.touchgrass

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.julieasoreng.touchgrass.ui.navigation.BloomNavHost
import com.julieasoreng.touchgrass.ui.theme.BloomTheme

class MainActivity : ComponentActivity() {

    private var showPostUnlock by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPostUnlock = intent.getBooleanExtra(EXTRA_SHOW_POST_UNLOCK, false)
        setContent {
            BloomTheme {
                BloomNavHost(
                    showPostUnlock = showPostUnlock,
                    onPostUnlockConsumed = { showPostUnlock = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_SHOW_POST_UNLOCK, false)) {
            showPostUnlock = true
        }
    }

    companion object {
        const val EXTRA_SHOW_POST_UNLOCK = "show_post_unlock"
    }
}
