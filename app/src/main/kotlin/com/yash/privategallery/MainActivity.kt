package com.yash.privategallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.repository.SettingsRepository
import com.yash.privategallery.ui.navigation.PrivateGalleryNavHost
import com.yash.privategallery.ui.theme.PrivateGalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Section 58: single-activity architecture — all 30 screens (Section 60) are
 * Compose destinations inside one NavHost, hosted by this one Activity. Uses
 * singleTask launch mode (see AndroidManifest) so re-launching the app from
 * the launcher never stacks a second instance on top of an already-running
 * one, which matters for the lock-on-background behavior in Section 5/45.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeViewModel: MainThemeViewModel = hiltViewModel()
            val settings by themeViewModel.settings.collectAsState()

            PrivateGalleryTheme(
                appTheme = settings?.theme ?: com.yash.privategallery.domain.model.AppTheme.SYSTEM_DEFAULT,
                useDynamicColor = settings?.useDynamicColor ?: true
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PrivateGalleryNavHost()
                }
            }
        }
    }
}

/**
 * Small Activity-scoped ViewModel purely to feed the theme (Section 36) from
 * DataStore-backed settings before the rest of the graph composes — kept
 * separate from any screen-specific ViewModel since theme applies globally.
 */
@HiltViewModel
class MainThemeViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val settings: StateFlow<com.yash.privategallery.domain.model.AppSettings?> =
        settingsRepository.observeSettings().stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
