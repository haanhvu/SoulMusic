package com.haanhvu.soulmusic

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

private var moreButtonClicked = 0

@Composable
fun SongItem(
    title: String,
    link: String
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = Int.MAX_VALUE,
            softWrap = true
        )

        Text(
            text = link,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ContextCompat.startActivity(context, intent, null)
                }
                //.padding(top = 4.dp),
        )
    }
}

@Composable
fun SongList(
    bakingViewModel: BakingViewModel
) {
    val stateMapRecordingTitleLink = remember { mutableStateMapOf<String, String>().apply { putAll(bakingViewModel.recordingTitleLink) } }
    val stateListRecordingTitleLink = remember { mutableStateListOf<Pair<String, String>>().apply { addAll(bakingViewModel.recordingTitleLink.toList()) } }
    var noMore by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(stateListRecordingTitleLink) { (title, link) ->
            SongItem(title, link)
        }

        /*if (bakingViewModel.showButton) {
            // Add the button as the last item
            item {
                Spacer(modifier = Modifier.height(16.dp)) // spacing before the button
                CenteredButton(bakingViewModel)
            }
        }*/

        item {
            if (!noMore) {
                Button(
                    onClick = {
                        moreButtonClicked++
                        if (moreButtonClicked > 3) {
                            noMore = true
                        } else {
                            bakingViewModel.addMoreResults(stateListRecordingTitleLink)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("More")
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text("The limit is set at 20 results per prompt in the alpha version.")
            }
        }
    }
}

/*@Composable
fun CenteredButton(
    bakingViewModel: BakingViewModel
) {
    var enabledAddingSongs by remember { mutableStateOf(false) }
    //var songsToShow by remember { mutableStateOf(emptyMap<String, String>()) }

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        if (bakingViewModel.showButton) {
            Button(onClick = {
                bakingViewModel.showButton = false
                enabledAddingSongs = true

                //songsToShow = bakingViewModel.recordingTitleLink
                //songsToShow = songsToShow + ("Title" to "https://example.com")
            }) {
                Text("More")
            }
        }
        if (enabledAddingSongs) {
            SongItem("New title", "New link")
            bakingViewModel.recordingTitleLink["New title"] = "New link"
        }
    }
    /*songsToShow.forEach { (title, link) ->
        SongItem(title, link) // ✅ now it's valid
    }*/
}*/

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BakingScreen(
    bakingViewModel: BakingViewModel = viewModel()
) {
    //val selectedImage = remember { mutableIntStateOf(0) }
    val placeholderPrompt = stringResource(R.string.prompt_placeholder)
    val placeholderResult = stringResource(R.string.results_placeholder)
    var prompt by rememberSaveable { mutableStateOf(placeholderPrompt) }
    var result by rememberSaveable { mutableStateOf(placeholderResult) }
    val uiState by bakingViewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.baking_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        Row(
            modifier = Modifier.padding(all = 16.dp)
        ) {
            TextField(
                value = prompt,
                label = { Text(stringResource(R.string.label_prompt)) },
                onValueChange = { prompt = it },
                modifier = Modifier
                    .weight(0.8f)
                    .padding(end = 16.dp)
                    .align(Alignment.CenterVertically)
            )

            Button(
                onClick = {
                    bakingViewModel.sendPrompt(prompt)
                },
                enabled = prompt.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                Text(text = stringResource(R.string.action_go))
            }
        }

        val buttonLabels = listOf("Yes", "Maybe", "Cancel", "Continue", "OK", "I'm finding answer")
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            buttonLabels.forEach { label ->
                Button(onClick = {
                    prompt = label
                    bakingViewModel.sendPrompt(prompt)
                }) {
                    Text(text = label)
                }
            }
        }

        if (uiState is UiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            var textColor = MaterialTheme.colorScheme.onSurface
            if (uiState is UiState.Error) {
                textColor = MaterialTheme.colorScheme.error
                result = (uiState as UiState.Error).errorMessage

                val scrollState = rememberScrollState()
                Text(
                    text = result,
                    textAlign = TextAlign.Start,
                    color = textColor,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                )
            } else if (uiState is UiState.Success) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SongList(bakingViewModel)
                }

                //textColor = MaterialTheme.colorScheme.onSurface
                //result = (uiState as UiState.Success).outputText
            }
            /*val scrollState = rememberScrollState()
            Text(
                text = result,
                textAlign = TextAlign.Start,
                color = textColor,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            )*/
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun BakingScreenPreview() {
    BakingScreen()
}