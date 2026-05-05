package fr.troupel.whereami.ui.options

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun OneUiSettingsRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean? = null,                 // if null → shows chevron; else → shows switch
    onToggle: ((Boolean) -> Unit)? = null,    // provide if using switch
    onClick: (() -> Unit)? = null,            // provide if navigates
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null || onToggle != null) {
                    when {
                        onToggle != null && checked != null -> onToggle(!checked)
                        onClick != null -> onClick()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    //.size(28.dp)
                    .padding(end = 14.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onToggle != null && checked != null) {
                // Switch on the right
                Switch(
                    checked = checked,
                    onCheckedChange = null // handled by Row click to get full-row toggle
                )
            } else {
                // Chevron for navigation (Material 3 doesn't ship a built-in, but > is fine or use Icons.Outlined.ChevronRight)
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun OneUiSettingsRowPreview() {
    var showBorders by remember { mutableStateOf(true) }

    Column {
        OneUiSettingsRow(
            icon = Icons.Default.Place,
            title = "Frontières",
            subtitle = "Toujours visibles, même pour les pays non essayés",
            checked = showBorders,
            onToggle = { showBorders = it }
        )
        Spacer(modifier=Modifier.height(16.dp))
        OneUiSettingsRow(
            icon = Icons.Default.Place,
            title = "Frontières",
            subtitle = "Toujours visibles, même pour les pays non essayés",
            checked = null,
            onToggle = { showBorders = it }
        )
    }
}