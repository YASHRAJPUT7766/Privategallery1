package com.yash.privategallery.ui.info

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.repository.MediaRepository
import com.yash.privategallery.domain.repository.PrivateMediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class ImageInfoUiState(
    val isLoading: Boolean = true,
    val fileName: String = "",
    val dateTimeLabel: String = "",
    val fileSizeLabel: String = "",
    val resolutionLabel: String = "",
    val fileType: String = "",
    val folderLabel: String = "",
    val mimeType: String = "",
    val locationLabel: String? = null,
    val isPrivate: Boolean = false
)

/**
 * Section 13/47: Image Info. For NORMAL items, EXIF (including GPS if
 * present and permitted) is read directly from the MediaStore-backed file.
 * For PRIVATE items, metadata comes only from the encrypted database row —
 * this screen is only reachable after Private Gallery authentication
 * already succeeded (nav-gated, same as every other private screen), so
 * "metadata should only be shown after authentication" (Section 47) holds
 * structurally rather than needing a second check here.
 *
 * GPS/location metadata is deliberately never logged (Section 47: "Do not
 * leak GPS/location metadata through logs") — it's read into UI state only,
 * and a decode failure is swallowed rather than surfaced as an exception
 * that might carry a file path into a crash log.
 */
@HiltViewModel
class ImageInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val privateMediaRepository: PrivateMediaRepository
) : ViewModel() {

    private val mediaId: Long = savedStateHandle.get<String>("mediaId")?.toLongOrNull() ?: -1L
    private val isPrivate: Boolean = savedStateHandle.get<String>("isPrivate")?.toBoolean() ?: false

    private val _uiState = MutableStateFlow(ImageInfoUiState())
    val uiState: StateFlow<ImageInfoUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val item = if (isPrivate) privateMediaRepository.getPrivateMediaById(mediaId) else mediaRepository.getMediaById(mediaId)
            if (item == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            var locationLabel: String? = null
            if (!isPrivate && item.contentUri != null) {
                locationLabel = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(Uri.parse(item.contentUri))?.use { stream ->
                            val exif = ExifInterface(stream)
                            val latLong = FloatArray(2)
                            if (exif.getLatLong(latLong)) "%.5f, %.5f".format(latLong[0], latLong[1]) else null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            _uiState.value = ImageInfoUiState(
                isLoading = false,
                fileName = item.displayName,
                dateTimeLabel = dateFormat.format(java.util.Date(item.dateTaken)),
                fileSizeLabel = formatFileSize(item.sizeBytes),
                resolutionLabel = "${item.width} × ${item.height}",
                fileType = item.mimeType.substringAfterLast('/').uppercase(),
                folderLabel = item.bucketName ?: if (isPrivate) "Private Gallery" else "—",
                mimeType = item.mimeType,
                locationLabel = locationLabel,
                isPrivate = isPrivate
            )
        }
    }

    private fun formatFileSize(bytes: Long): String = when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
