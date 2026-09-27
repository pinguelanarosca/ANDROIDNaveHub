package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NaveHubApp
import com.example.ui.NaveHubViewModel
import com.example.ui.theme.NaveHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NaveHubTheme {
                val viewModel: NaveHubViewModel = viewModel()
                NaveHubApp(viewModel = viewModel)
            }
        }
    }
}
