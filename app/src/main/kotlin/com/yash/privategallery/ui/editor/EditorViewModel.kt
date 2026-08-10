package com.yash.privategallery.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.core.editor.ImageEditEngine
import com.yash.privategallery.domain.model.EditOperation
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.model.SaveMode
import com.yash.privategallery.domain.model.StorageLocation
import com.yash.privategallery.domain.repository.ImportResult
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
import javax.inject.Inject

enum class EditorTool { CROP, ADJUST, FILTERS, DRAW, NONE }

data class EditorUiState(
    val isLoading: Boolean = true,
    val sourceBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val operations: List<EditOperation> = emptyList(),
    val redoStack: List<EditOperation> = emptyList(),
    val activeTool: EditorTool = EditorTool.NONE,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val errorMessage: String? = null,
    val isPrivate: Boolean = false
) {
    val canUndo: Boolean get() = operations.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}

/**
 * Section 11/12's editor logic. The operation list IS the undo/redo history
 * (Section 11: "Support multiple undo/redo operations") — undo pops the last
 * operation onto [EditorUiState.redoStack], redo pops it back; both trigger
 * a full re-render via [ImageEditEngine] from the untouched source bitmap,
 * which is what makes repeated undo/redo cycles lossless.
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val privateMediaRepository: PrivateMediaRepository,
    private val editEngine: ImageEditEngine
) : ViewModel() {

    private val mediaId: Long = savedStateHandle.get<String>("mediaId")?.toLongOrNull() ?: -1L

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var sourceMediaItem: MediaItem? = null

    init {
        loadSource()
    }

    private fun loadSource() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val item = mediaRepository.getMediaById(mediaId) ?: privateMediaRepository.getPrivateMediaById(mediaId)
            if (item == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Could not open this image for editing.")
                return@launch
            }
            sourceMediaItem = item

            val bitmap = decodeBitmap(item)
            if (bitmap == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Could not decode this image.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                sourceBitmap = bitmap,
                previewBitmap = bitmap,
                isPrivate = item.storageLocation == StorageLocation.PRIVATE
            )
        }
    }

    private suspend fun decodeBitmap(item: MediaItem): Bitmap? = withContext(Dispatchers.IO) {
        try {
            when {
                item.contentUri != null -> context.contentResolver.openInputStream(Uri.parse(item.contentUri))?.use {
                    android.graphics.BitmapFactory.decodeStream(it)
                }
                item.filePath != null -> android.graphics.BitmapFactory.decodeFile(item.filePath)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setActiveTool(tool: EditorTool) {
        _uiState.value = _uiState.value.copy(activeTool = if (_uiState.value.activeTool == tool) EditorTool.NONE else tool)
    }

    /** Pushes a new operation onto the stack and clears any redo history (standard undo/redo semantics). */
    fun applyOperation(operation: EditOperation) {
        val newOps = _uiState.value.operations + operation
        _uiState.value = _uiState.value.copy(operations = newOps, redoStack = emptyList())
        rerender()
    }

    /**
     * Replaces the most recent operation of the same runtime type instead of
     * appending — used while a slider is actively being dragged, so dragging
     * "Brightness" doesn't push hundreds of undo-stack entries, one per
     * pixel of drag. A discrete "commit" (e.g. releasing the slider) should
     * call [applyOperation] instead to create a real undo checkpoint.
     */
    fun previewOperation(operation: EditOperation) {
        val current = _uiState.value.operations
        val withoutLastOfSameType = if (current.isNotEmpty() && current.last()::class == operation::class) {
            current.dropLast(1)
        } else current
        _uiState.value = _uiState.value.copy(operations = withoutLastOfSameType + operation)
        rerender()
    }

    fun undo() {
        val current = _uiState.value
        if (current.operations.isEmpty()) return
        val lastOp = current.operations.last()
        _uiState.value = current.copy(
            operations = current.operations.dropLast(1),
            redoStack = current.redoStack + lastOp
        )
        rerender()
    }

    fun redo() {
        val current = _uiState.value
        if (current.redoStack.isEmpty()) return
        val nextOp = current.redoStack.last()
        _uiState.value = current.copy(
            operations = current.operations + nextOp,
            redoStack = current.redoStack.dropLast(1)
        )
        rerender()
    }

    private fun rerender() {
        val source = _uiState.value.sourceBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val rendered = editEngine.render(source, _uiState.value.operations)
            _uiState.value = _uiState.value.copy(previewBitmap = rendered)
        }
    }

    /**
     * Section 12: "Save changes" — Replace original / Save as new / Cancel.
     * For PRIVATE items, the edited result is re-encrypted and stored back
     * into the vault (Section 12: "For Private Gallery, edited private
     * images must remain private") rather than ever touching public
     * MediaStore, regardless of [saveMode].
     */
    fun save(saveMode: SaveMode, onComplete: (success: Boolean) -> Unit) {
        val item = sourceMediaItem ?: return onComplete(false)
        val finalBitmap = _uiState.value.previewBitmap ?: return onComplete(false)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val success = when (item.storageLocation) {
                StorageLocation.PRIVATE -> saveEditedPrivateImage(item, finalBitmap)
                StorageLocation.NORMAL -> withContext(Dispatchers.IO) {
                    try {
                        saveEditedNormalImage(item, finalBitmap, saveMode)
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            _uiState.value = _uiState.value.copy(isSaving = false, saveComplete = success)
            onComplete(success)
        }
    }

    private fun saveEditedNormalImage(item: MediaItem, bitmap: Bitmap, saveMode: SaveMode): Boolean {
        val values = android.content.ContentValues().apply {
            val name = if (saveMode == SaveMode.SAVE_AS_NEW) "edit_${System.currentTimeMillis()}_${item.displayName}" else item.displayName
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        val targetUri = if (saveMode == SaveMode.REPLACE_ORIGINAL && item.contentUri != null) {
            Uri.parse(item.contentUri)
        } else {
            context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } ?: return false

        context.contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        } ?: return false
        return true
    }

    /**
     * Private edits never leave the vault: the finished bitmap is
     * re-encrypted via the same import path new vault imports use
     * ([PrivateMediaRepository.importToPrivate], which itself performs
     * encrypt→verify before committing), and the pre-edit copy is only
     * trashed after the new encrypted copy is confirmed present — mirroring
     * Section 57's safety ordering applied to edit-replace.
     */
    private suspend fun saveEditedPrivateImage(item: MediaItem, bitmap: Bitmap): Boolean {
        val tempFile = java.io.File(context.cacheDir, "edit_temp_${System.currentTimeMillis()}.jpg")
        return try {
            withContext(Dispatchers.IO) {
                tempFile.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) }
            }
            val results = privateMediaRepository.importToPrivate(listOf(Uri.fromFile(tempFile).toString()))
            val success = results.firstOrNull() is ImportResult.Success
            if (success) {
                privateMediaRepository.moveToPrivateTrash(listOf(item.id))
            }
            success
        } finally {
            tempFile.delete()
        }
    }
}
