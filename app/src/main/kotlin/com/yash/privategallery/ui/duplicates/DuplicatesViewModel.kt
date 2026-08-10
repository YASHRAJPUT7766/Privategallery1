package com.yash.privategallery.ui.duplicates

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.model.MediaType
import com.yash.privategallery.domain.repository.MediaRepository
import com.yash.privategallery.domain.repository.SortOrder
import com.yash.privategallery.domain.usecase.DetectDuplicatesUseCase
import com.yash.privategallery.domain.usecase.DuplicateGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject

data class DuplicatesUiState(
    val isScanning: Boolean = false,
    val groups: List<DuplicateGroup> = emptyList(),
    val selectedForDeletion: Set<Long> = emptySet(),
    val hasScanned: Boolean = false
)

/**
 * Section 28: runs [DetectDuplicatesUseCase] against the current library,
 * providing real (not stubbed) hash functions:
 *  - file hash: streaming SHA-256 over the actual file bytes (exact
 *    byte-identical duplicate detection).
 *  - perceptual hash: a simple 8x8 grayscale average-hash (aHash) — resize
 *    to 8x8, compare each pixel to the mean, one bit per pixel, giving a
 *    64-bit fingerprint. Cheap enough to compute for every image not
 *    already grouped by exact hash, and standard enough to catch
 *    near-duplicates (re-compressed, minor crops/edits).
 *
 * Never auto-deletes (Section 28: "Never automatically delete duplicates
 * without confirmation") — [confirmDeleteSelected] is the only path that
 * removes anything, and it moves items to trash (soft delete) rather than
 * permanently deleting, consistent with Section 27's safety-first deletion
 * flow everywhere else in the app.
 */
@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val detectDuplicates: DetectDuplicatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    /**
     * A duplicate scan is a deliberate, point-in-time operation the user
     * re-runs explicitly — it takes one snapshot of the library rather than
     * continuing to observe live MediaStore changes, since re-hashing on
     * every change mid-scan would be wasteful and produce a janky UI.
     */
    fun scanForDuplicates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)

            val snapshot = mediaRepository.observeAllMedia(SortOrder.NEWEST_FIRST).first()
            val imagesOnly = snapshot.filter { it.mediaType == MediaType.IMAGE }

            val groups = withContext(Dispatchers.Default) {
                detectDuplicates(
                    items = imagesOnly,
                    fileHashProvider = { item -> computeFileHash(item) },
                    perceptualHashProvider = { item -> computePerceptualHash(item) }
                )
            }
            _uiState.value = _uiState.value.copy(isScanning = false, groups = groups, hasScanned = true)
        }
    }

    fun toggleSelectionForDeletion(mediaId: Long) {
        val current = _uiState.value.selectedForDeletion
        _uiState.value = _uiState.value.copy(
            selectedForDeletion = if (mediaId in current) current - mediaId else current + mediaId
        )
    }

    /** Section 28: "Keep one" — selects every item in a group except the first, a quick default. */
    fun selectAllButFirstInGroup(group: DuplicateGroup) {
        val idsToSelect = group.items.drop(1).map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedForDeletion = _uiState.value.selectedForDeletion + idsToSelect)
    }

    /** Confirmed deletion (Section 28's explicit confirmation requirement) — soft-deletes to trash. */
    fun confirmDeleteSelected(onComplete: () -> Unit) {
        val ids = _uiState.value.selectedForDeletion.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            mediaRepository.moveToTrash(ids)
            _uiState.value = _uiState.value.copy(
                selectedForDeletion = emptySet(),
                groups = _uiState.value.groups
                    .map { group -> group.copy(items = group.items.filter { it.id !in ids }) }
                    .filter { it.items.size > 1 }
            )
            onComplete()
        }
    }

    private fun computeFileHash(item: MediaItem): String? = try {
        val digest = MessageDigest.getInstance("SHA-256")
        val uri = item.contentUri?.let { Uri.parse(it) }
        val stream = if (uri != null) context.contentResolver.openInputStream(uri) else item.filePath?.let { java.io.File(it).inputStream() }
        stream?.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        null
    }

    private fun computePerceptualHash(item: MediaItem): Long? = try {
        val uri = item.contentUri?.let { Uri.parse(it) }
        val bitmap: Bitmap? = if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { decodeScaledDownsample(it) }
        } else {
            item.filePath?.let { BitmapFactory.decodeFile(it) }
        }
        bitmap?.let { averageHash(it) }
    } catch (e: Exception) {
        null
    }

    private fun decodeScaledDownsample(input: java.io.InputStream): Bitmap? {
        val bytes = input.readBytes()
        val options = BitmapFactory.Options().apply { inSampleSize = 8 } // fast downsample before the 8x8 resize below
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    /** Classic 8x8 average-hash (aHash): resize to 8x8 grayscale, 1 bit per pixel vs. the mean. */
    private fun averageHash(source: Bitmap): Long {
        val small = Bitmap.createScaledBitmap(source, 8, 8, true)
        val grays = IntArray(64)
        var sum = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val pixel = small.getPixel(x, y)
                val gray = (android.graphics.Color.red(pixel) + android.graphics.Color.green(pixel) + android.graphics.Color.blue(pixel)) / 3
                grays[y * 8 + x] = gray
                sum += gray
            }
        }
        val mean = sum / 64
        var hash = 0L
        for (i in 0 until 64) {
            if (grays[i] >= mean) hash = hash or (1L shl i)
        }
        return hash
    }
}
