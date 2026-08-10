package com.yash.privategallery.ui.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.yash.privategallery.domain.model.SaveMode

/** Section 12: "Save changes" dialog — Replace original / Save as new / Cancel. */
@Composable
fun SaveChangesDialog(
    onSave: (SaveMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save changes") },
        text = { Text("Choose how you'd like to save this edit.") },
        confirmButton = {
            TextButton(onClick = { onSave(SaveMode.REPLACE_ORIGINAL) }) {
                Text("Replace original")
            }
        },
        dismissButton = {
            TextButton(onClick = { onSave(SaveMode.SAVE_AS_NEW) }) {
                Text("Save as new")
            }
        }
    )
}
