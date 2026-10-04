package dev.jordanempire.youflow.ui.search

import dev.jordanempire.youflow.ui.util.toUiError
import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jordanempire.youflow.ui.AppActions
import dev.jordanempire.youflow.ui.components.ChannelRow
import dev.jordanempire.youflow.ui.components.LoadingBox
import dev.jordanempire.youflow.ui.components.ErrorBox
import dev.jordanempire.youflow.ui.components.MessageBox
import dev.jordanempire.youflow.ui.components.PlaylistRow
import dev.jordanempire.youflow.ui.components.VideoCard
import dev.jordanempire.youflow.ui.data.YouTubeRepository
import dev.jordanempire.youflow.ui.model.ChannelItem
import dev.jordanempire.youflow.ui.model.ContentItem
import dev.jordanempire.youflow.ui.model.PlaylistItem
import dev.jordanempire.youflow.ui.model.UiState
import dev.jordanempire.youflow.ui.model.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = YouTubeRepository(app)

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()
    private val _results = MutableStateFlow<UiState<List<ContentItem>>?>(null)
    val results = _results.asStateFlow()

    val subscribedUrls: StateFlow<Set<String>> = repo.subscriptions()
        .map { list -> list.map { it.url }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _query.debounce(250).distinctUntilChanged().collect { q ->
                _suggestions.value = if (q.isBlank()) emptyList() else runCatching { repo.suggestions(q) }.getOrDefault(emptyList())
            }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        if (_results.value != null) _results.value = null
    }

    fun submit(value: String = _query.value) {
        val q = value.trim()
        if (q.isEmpty()) return
        _query.value = q
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _results.value = UiState.Loading
            _results.value = try {
                UiState.Content(repo.search(q))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.toUiError()
            }
        }
    }

    fun toggleSubscribe(channel: ChannelItem) {
        viewModelScope.launch {
            if (channel.url in subscribedUrls.value) repo.unsubscribe(channel.url) else repo.subscribe(channel)
        }
    }
}

@Composable
fun SearchScreen(actions: AppActions, onClose: () -> Unit, vm: SearchViewModel = viewModel()) {
    val query by vm.query.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val results by vm.results.collectAsState()
    val subscribed by vm.subscribedUrls.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler(onBack = onClose)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = vm::onQueryChange,
                        placeholder = { Text("Search YouTube") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { vm.submit() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    )
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Outlined.Close, contentDescription = "Clear") }
                    }
                }
            )
        }
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (val r = results) {
            null -> LazyColumn(modifier) {
                items(suggestions, key = { it }) { suggestion ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.submit(suggestion) }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(suggestion, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
            UiState.Loading -> LoadingBox(modifier)
            is UiState.Error -> ErrorBox(r, { vm.submit() }, modifier, "Search failed")
            is UiState.Content -> if (r.data.isEmpty()) {
                MessageBox("No results", "Try different words.", modifier = modifier)
            } else {
                LazyColumn(modifier) {
                    items(r.data, key = { it.url }) { item ->
                        when (item) {
                            is VideoItem -> VideoCard(item, { actions.openVideo(item) }, { item.channelUrl?.let(actions.openChannel) }, onSave = { actions.saveVideo(item) })
                            is ChannelItem -> ChannelRow(item, item.url in subscribed, { actions.openChannel(item.url) }, { vm.toggleSubscribe(item) })
                            is PlaylistItem -> PlaylistRow(item, { actions.openPlaylist(item.url) })
                        }
                    }
                }
            }
        }
    }
}
