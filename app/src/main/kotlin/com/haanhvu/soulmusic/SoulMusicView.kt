package com.haanhvu.soulmusic

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

private var moreButtonClicked = 0

@Composable
fun IconRowWithEmojis() {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(text = "🎶", fontSize = 20.sp)
        Text(text = "🎤", fontSize = 20.sp)
        Text(text = "🔗", fontSize = 20.sp)
    }
}

@Composable
fun SongItem(
    titleArtist: String,
    link: String
) {
    val context = LocalContext.current

    val ltrMark = "\u200E"

    val titleArtistList = titleArtist.split(" - ")

    val title = titleArtistList[0]
    val artist = titleArtistList[1]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "$ltrMark$title",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
                maxLines = Int.MAX_VALUE,
                softWrap = true
            )
        }

        Text(
            text = artist,
            style = MaterialTheme.typography.titleMedium,
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.Start
        )

        Text(
            text = "Youtube",
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .weight(1f)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ContextCompat.startActivity(context, intent, null)
                }
        )
    }
}

@Composable
fun SongList(
    soulMusicViewModel: SoulMusicViewModel
) {
    val stateListRecordingTitleLink = remember { mutableStateListOf<Pair<String, String>>().apply { addAll(soulMusicViewModel.recordingTitleLink.toList()) } }
    var noMore by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(stateListRecordingTitleLink) { (title, link) ->
            SongItem(title, link)
        }

        item {
            if (!noMore) {
                Button(
                    onClick = {
                        moreButtonClicked++
                        if (moreButtonClicked > 3) {
                            noMore = true
                        } else {
                            soulMusicViewModel.addMoreResults(moreButtonClicked, stateListRecordingTitleLink)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoulMusicView(
    apiKey: String,
    soulMusicViewModel: SoulMusicViewModel = viewModel()
) {
    soulMusicViewModel.apiKey = apiKey
    val placeholderResult = stringResource(R.string.results_placeholder)
    var prompt by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf(placeholderResult) }
    val uiState by soulMusicViewModel.uiState.collectAsState()
    val options = listOf("Popular", "Hidden gems")
    var selectedOption by remember { mutableStateOf(options[0]) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Automatically request focus when the composable enters the composition
    LaunchedEffect(Unit) {
        // optional delay can help in some cases to ensure the view is ready
        delay(100)
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.app_name),
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
                placeholder = { Text(stringResource(R.string.prompt_placeholder)) },
                modifier = Modifier
                    .weight(0.8f)
                    .padding(end = 16.dp)
                    .align(Alignment.CenterVertically)
                    .focusRequester(focusRequester)
            )

            Button(
                onClick = {
                    moreButtonClicked = 0
                    focusManager.clearFocus()
                    if (selectedOption.equals(options[1])) {
                        soulMusicViewModel.sendPromptToGetHiddenGems(prompt)
                    } else if (selectedOption.equals(options[0])) {
                        soulMusicViewModel.sendPromptToGetPopularResults(prompt)
                    }
                },
                enabled = prompt.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                Text(text = stringResource(R.string.action_go))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (option == selectedOption),
                        onClick = { selectedOption = option }
                    )
                    Text(option)
                }
            }
        }

        val buttonLabels = listOf(
            "I feel lost. I don't know what to do. Give me clarity",
            "I'm tired. Boost my energy up",
            "I need motivation",
            "Clean my negative energy",
            "I need comfort",)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            buttonLabels.forEach { label ->
                Button(onClick = {
                    moreButtonClicked = 0
                    focusManager.clearFocus()
                    prompt = label
                    if (selectedOption.equals(options[1])) {
                        soulMusicViewModel.sendPromptToGetHiddenGems(prompt)
                    } else if (selectedOption.equals(options[0])) {
                        soulMusicViewModel.sendPromptToGetPopularResults(prompt)
                    }
                }) {
                    Text(text = label)
                }
            }
        }

        if (uiState is UiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            if (uiState is UiState.Error) {
                val textColor = MaterialTheme.colorScheme.error
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
                IconRowWithEmojis()
                Column(modifier = Modifier.fillMaxSize()) {
                    SongList(soulMusicViewModel)
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun SoulMusicPreview() {
    SoulMusicView("test")
}