package com.learntogether.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.learntogether.app.data.AppViewModel
import com.learntogether.app.ui.LearnTogetherApp
import com.learntogether.app.ui.theme.LearnTogetherTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LearnTogetherTheme(highContrast = vm.highContrast, textScale = vm.textScale) {
                LearnTogetherApp(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.motion.resume()
    }

    override fun onPause() {
        vm.motion.pause()
        super.onPause()
    }
}
