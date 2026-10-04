package dev.jordanempire.youflow.ui.search

import dev.jordanempire.youflow.ui.util.toUiError
import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.snapshotFlow
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
    private var next: org.schabi.newpipe.extractor.Page? = null
    private var loadingMore = false

    private val _filter = MutableStateFlow<String?>(null)
    val filter = _filter.asStateFlow()
    private val _correction = MutableStateFlow<Pair<String, Boolean>?>(null)
    /** Suggested query and whether the shown results are already for the corrected text. */
    val correction = _correction.asStateFlow()

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

    fun setFilter(filter: String?) {
        if (_filter.value == filter) return
        _filter.value = filter
        submit()
    }

    fun loadMore() {
        val page = next ?: return
        val current = (_results.value as? UiState.Content)?.data ?: return
        if (loadingMore) return
        loadingMore = true
        viewModelScope.launch {
            try {
                val more = repo.moreSearch(_query.value, _filter.value, page)
                next = more.next
                _results.value = UiState.Content((current + more.items).distinctBy { it.url })
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                next = null
            }
            loadingMore = false
        }
    }

    fun submit(value: String = _query.value) {
        val q = value.trim()
        if (q.isEmpty()) return
        _query.value = q
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _results.value = UiState.Loading
            _correction.value = null
            _results.value = try {
                val page = repo.search(q, _filter.value)
                next = page.next
                _correction.value = page.suggestion?.takeIf { it.isNotBlank() }?.let { it to page.corrected }
                UiState.Content(page.items)
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

private val SearchFilters = listOf("All" to null, "Videos" to "videos", "Channels" to "channels", "Playlists" to "playlists")

@Composable
fun SearchScreen(actions: AppActions, onClose: () -> Unit, initialQuery: String? = null, vm: SearchViewModel = viewModel()) {
    val query by vm.query.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val results by vm.results.collectAsState()
    val subscribed by vm.subscribedUrls.collectAsState()
    val filter by vm.filter.collectAsState()
    val correction by vm.correction.collectAsState()
    val resultsState = rememberLazyListState()
    LaunchedEffect(resultsState) {
        snapshotFlow { resultsState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to resultsState.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 4) vm.loadMore() }
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(initialQuery) {
        if (initialQuery != null) {
            vm.onQueryChange(initialQuery)
            vm.submit(initialQuery)
        } else {
            focus.requestFocus()
        }
    }
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
                LazyColumn(modifier, state = resultsState) {
                    item("filters") {
                        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SearchFilters) { (label, value) ->
                                FilterChip(selected = filter == value, onClick = { vm.setFilter(value) }, label = { Text(label) })
                            }
                        }
                    }
                    correction?.let { (suggestion, corrected) ->
                        item("correction") {
                            Text(
                                if (corrected) "Showing results for $suggestion" else "Did you mean $suggestion?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().clickable { vm.submit(suggestion) }.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(r.data, key = { it.url }) { item ->
                        when (item) {
                            is VideoItem -> VideoCard(item, { actions.openVideo(item) }, { item.channelUrl?.let(actions.openChannel) }, onSave = { actions.saveVideo(item) }, onPlayNext = { actions.playNext(item) }, onEnqueue = { actions.enqueue(item) })
                            is ChannelItem -> ChannelRow(item, item.url in subscribed, { actions.openChannel(item.url) }, { vm.toggleSubscribe(item) })
                            is PlaylistItem -> PlaylistRow(item, { actions.openPlaylist(item.url) })
                        }
                    }
                }
            }
        }
    }
}
