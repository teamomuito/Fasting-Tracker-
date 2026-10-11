package app.fasting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import app.fasting.ui.FastingRoot
import app.fasting.ui.FastingViewModel
import app.fasting.ui.theme.DsColors
import app.fasting.ui.theme.FastingTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(DsColors.TopDark.toArgb()),
            navigationBarStyle = SystemBarStyle.light(DsColors.Bar.toArgb(), DsColors.Bar.toArgb()),
        )
        val repository = (application as FastingApplication).container.repository
        setContent {
            FastingTheme {
                val vm: FastingViewModel = viewModel(factory = FastingViewModel.factory(repository))
                FastingRoot(vm)
            }
        }
    }
}
