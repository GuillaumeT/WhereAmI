package fr.troupel.whereami.ui.guesscountry.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.troupel.whereami.data.model.Country
import androidx.compose.material3.Icon
import fr.troupel.whereami.ui.CountryGuessResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryInput(
    modifier: Modifier = Modifier,
    label: String = "Trouve le pays mystère",
    onSubmit: (String) -> CountryGuessResult,
    onWin: () -> Unit,
    onValidGuess: (Country) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf(listOf<Country>()) }

    fun submit(value: String) {
        text = value // most probably already the case but not in case of suggestion selection
        val result = onSubmit(text)
        if (result.isCountryGuessed) {
            onWin()
        }
        if (result.country != null) {
            text = ""
            expanded = false
            onValidGuess(result.country)
        } else {
            expanded = true
            searchResults = result.suggestions
        }
    }

    Box(modifier = modifier) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                TextField(
                    value = text,
                    onValueChange = { searchText: String -> text = searchText },
                    label = { Text(label) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Password,
                        showKeyboardOnFocus = false,
                    ),
                    keyboardActions = KeyboardActions(onSearch = { submit(text) }),
                    shape = RoundedCornerShape(15.dp, 15.dp),
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {
                if (searchResults.isEmpty())
                    Text("Aucune correspondance trouvée")
                else {
                    Log.d("Guess", "$searchResults")
                    searchResults.forEach { result ->
                        ListItem(
                            headlineContent = { Text(result.name) },
                            modifier = Modifier
                                .clickable { submit(result.name) }
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

