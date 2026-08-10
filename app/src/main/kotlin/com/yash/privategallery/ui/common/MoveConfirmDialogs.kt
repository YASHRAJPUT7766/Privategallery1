package com.yash.privategallery.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Section 21: "Show confirmation: 'These items will be removed from the
 * normal gallery and stored securely in Private Gallery.'" This dialog is
 * purely the confirmation step — the actual move (with its copy→verify→
 * delete safety ordering) happens in
 * [com.yash.privategallery.domain.repository.PrivateMediaRepository.moveToPrivate]
 * only after the user confirms here.
 */
@Composable
fun MoveToPrivateConfirmDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Private Gallery") },
        text = {
            Text(
                "These $itemCount item(s) will be removed from the normal gallery " +
                    "and stored securely in Private Gallery. This can be undone later " +
                    "from Private Gallery's \"Move to Gallery\" option."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Move") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Section 23's mirror-image confirmation for restoring private items back to the normal gallery. */
@Composable
fun MoveToNormalConfirmDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Gallery") },
        text = {
            Text("These $itemCount item(s) will be moved out of Private Gallery and become visible in your normal gallery again.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Move") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
