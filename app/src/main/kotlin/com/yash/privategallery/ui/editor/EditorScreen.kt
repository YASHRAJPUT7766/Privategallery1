package com.yash.privategallery.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yash.privategallery.domain.model.CropRatio
import com.yash.privategallery.domain.model.EditOperation
import com.yash.privategallery.ui.common.EmptyState
import com.yash.privategallery.ui.common.SecureScreenEffect

private enum class EditorTab { BASIC, ADJUST, FILTERS, DRAW }

/**
 * Section 11-13: the full image editor. A single top-level tool switcher
 * (Basic/Adjust/Filters/Draw) rather than everything visible at once, since
 * a mobile screen can't fit crop handles + 13 sliders + a filter strip + a
 * markup toolbar simultaneously — matches how production editors (and
 * Section 37's "modern and minimal... proper spacing" guidance) handle this.
 */
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isPrivate) {
        SecureScreenEffect()
    }
    var selectedTab by remember { mutableStateOf(EditorTab.BASIC) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var selectedCropRatio by remember { mutableStateOf(CropRatio.FREE) }
    var selectedMarkupTool by remember { mutableStateOf(MarkupTool.PEN) }
    val markupColor = remember { Color.Red }

    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = uiState.canUndo) {
                        Icon(Icons.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = uiState.canRedo) {
                        Icon(Icons.Filled.Redo, contentDescription = "Redo")
                    }
                    TextButton(onClick = { showSaveDialog = true }, enabled = !uiState.isSaving) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.errorMessage != null -> EmptyState(message = uiState.errorMessage ?: "", modifier = Modifier.padding(paddingValues))
            else -> Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    uiState.previewBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    when (selectedTab) {
                        EditorTab.BASIC -> CropOverlay(
                            selectedRatio = selectedCropRatio,
                            onRatioSelected = { selectedCropRatio = it },
                            onCropChanged = { l, t, r, b ->
                                viewModel.applyOperation(EditOperation.Crop(l, t, r, b))
                            }
                        )
                        EditorTab.DRAW -> MarkupCanvas(
                            activeTool = selectedMarkupTool,
                            color = markupColor,
                            strokeWidth = 8f,
                            onStrokeComplete = { viewModel.applyOperation(it) }
                        )
                        else -> Unit
                    }

                    if (uiState.isSaving) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                when (selectedTab) {
                    EditorTab.ADJUST -> {
                        val currentAdjust = uiState.operations.filterIsInstance<EditOperation.Adjust>().lastOrNull() ?: EditOperation.Adjust()
                        AdjustPanel(
                            currentAdjust = currentAdjust,
                            onPreview = { viewModel.previewOperation(it) },
                            onCommit = { viewModel.applyOperation(it) },
                            modifier = Modifier.height(240.dp)
                        )
                    }
                    EditorTab.FILTERS -> {
                        val currentFilter = uiState.operations.filterIsInstance<EditOperation.ApplyFilter>().lastOrNull()?.filterId ?: "none"
                        FilterStrip(
                            selectedFilterId = currentFilter,
                            onFilterSelected = { viewModel.applyOperation(EditOperation.ApplyFilter(it)) }
                        )
                    }
                    EditorTab.DRAW -> MarkupToolbar(
                        selectedTool = selectedMarkupTool,
                        onToolSelected = { selectedMarkupTool = it }
                    )
                    EditorTab.BASIC -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(onClick = { viewModel.applyOperation(EditOperation.Rotate(90f)) }) { Text("Rotate") }
                            TextButton(onClick = { viewModel.applyOperation(EditOperation.FlipHorizontal) }) { Text("Flip H") }
                            TextButton(onClick = { viewModel.applyOperation(EditOperation.FlipVertical) }) { Text("Flip V") }
                        }
                    }
                }

                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    Tab(selected = selectedTab == EditorTab.BASIC, onClick = { selectedTab = EditorTab.BASIC }, text = { Text("Basic") })
                    Tab(selected = selectedTab == EditorTab.ADJUST, onClick = { selectedTab = EditorTab.ADJUST }, text = { Text("Adjust") })
                    Tab(selected = selectedTab == EditorTab.FILTERS, onClick = { selectedTab = EditorTab.FILTERS }, text = { Text("Filters") })
                    Tab(selected = selectedTab == EditorTab.DRAW, onClick = { selectedTab = EditorTab.DRAW }, text = { Text("Draw") })
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveChangesDialog(
            onSave = { saveMode ->
                showSaveDialog = false
                viewModel.save(saveMode) { }
            },
            onDismiss = { showSaveDialog = false }
        )
    }
}
