package com.yash.privategallery.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.yash.privategallery.core.editor.FilterPresets

/** Section 11's Filters strip — tap a preset to apply it. */
@Composable
fun FilterStrip(
    selectedFilterId: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        items(FilterPresets.available) { preset ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onFilterSelected(preset.id) }
            ) {
                val isSelected = preset.id == selectedFilterId
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
