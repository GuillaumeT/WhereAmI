package fr.troupel.whereami.ui.options

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(onBack: () -> Unit) {
    var difficulty by remember { mutableIntStateOf(1) }
    var showBorders by remember { mutableStateOf(true) }
    var enableAfrica by remember { mutableStateOf(true) }
    var enableAmerica by remember { mutableStateOf(true) }
    var enableAsia by remember { mutableStateOf(true) }
    var enableEuropa by remember { mutableStateOf(true) }
    var enableOceania by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // space between each child
    ) {
        TopAppBar( modifier = Modifier.fillMaxWidth(), title = {
                Text(
                    "Préférences",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Retour")
                }
            },
            actions = {
                // keep empty so the title centering isn't shifted
                Spacer(Modifier.width(48.dp)) // balance the IconButton width if needed
            }
        )
        SettingsGroup(title = "Difficulté") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf("facile", "moyen", "difficile")
                options.forEachIndexed { i, label ->
                    SegmentedButton(shape = SegmentedButtonDefaults.itemShape(
                        index = i,
                        count = options.size
                    ),
                        selected = difficulty == i, onClick = { difficulty = i }) { Text(label) }
                }
            }
        }
        SettingsGroup {
            SettingsToggle(
                title = "Frontières",
                subtitle = "Afficher les frontières",
                checked = showBorders,
            ) { showBorders = it }
        }
        SettingsGroup(title = "Zones géographiques") {
            SettingsToggle(
                title = "Afrique",
                checked = enableAfrica,
            ) { enableAfrica = it }
            SettingsGroupSeparator()
            SettingsToggle(
                title = "Amérique",
                checked = enableAmerica
            ) { enableAmerica = it }
            SettingsGroupSeparator()
            SettingsToggle(
                title = "Asie",
                checked = enableAsia
            ) { enableAsia = it }
            SettingsGroupSeparator()
            SettingsToggle(
                title = "Europe",
                checked = enableEuropa
            ) { enableEuropa = it }
            SettingsGroupSeparator()
            SettingsToggle(
                title = "Océanie",
                checked = enableOceania
            ) { enableOceania = it }
        }
    }
}

@Composable
fun SettingsToggle(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = { onCheckedChange(!checked) },
                role = Role.Switch,
                interactionSource = interaction,
                indication = null,
            ),
        //.clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = null, // Row handles toggle
            interactionSource = interaction
        )
    }
}


@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    items: @Composable ColumnScope.() -> Unit
) {
    Column {
        if (title !== null) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
                text = title,
            )
        }
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp,
        ) {
            Column(modifier = modifier.padding(16.dp)) {
                items()
            }
        }
    }
}

@Composable
fun SettingsGroupSeparator(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier.padding(8.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = .2f)
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsGroupPreview() {
    var checked by remember { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsGroup(title = "First group") {
            SettingsToggle(
                title = "Title",
                subtitle = "A short explanation",
                checked = checked,
            ) { checked = it }
            SettingsGroupSeparator()
            SettingsToggle(
                title = "Title",
                subtitle = "A short explanation",
                checked = checked,
            ) { checked = it }
        }
        SettingsGroup() {
            SettingsToggle(
                title = "Title",
                subtitle = "A short explanation",
                checked = checked,
            ) { checked = it }
            SettingsGroupSeparator()
            SettingsToggle(
                title = "Title",
                subtitle = "A short explanation",
                checked = checked,
            ) { checked = it }
            SettingsGroupSeparator()
            SettingsToggle(
                title = "Title",
                subtitle = "A short explanation",
                checked = checked,
            ) { checked = it }
            SettingsGroupSeparator()
            SettingsToggle(
                title = "Title",
                subtitle = "A short explanation",
                checked = checked,
            ) { checked = it }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OptionsScreenPreview() {
    OptionsScreen { }
}

