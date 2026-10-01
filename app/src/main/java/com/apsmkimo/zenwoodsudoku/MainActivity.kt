package com.apsmkimo.zenwoodsudoku

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apsmkimo.zenwoodsudoku.ui.ZenWoodRoot
import com.apsmkimo.zenwoodsudoku.ui.bootstrap.BootstrapViewModel
import com.apsmkimo.zenwoodsudoku.ui.theme.ZenWoodTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val repository = (application as ZenWoodApplication).container.repository
        val bootstrapViewModel: BootstrapViewModel by viewModels {
            factory { BootstrapViewModel(repository) }
        }
        setContent {
            ZenWoodTheme {
                ZenWoodRoot(
                    bootstrapViewModel = bootstrapViewModel,
                    repository = repository,
                )
            }
        }
    }
}

private inline fun <reified T : ViewModel> factory(
    crossinline create: () -> T,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
