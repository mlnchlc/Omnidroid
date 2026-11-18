package com.omnidroid.app.mobile.feature.library

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omnidroid.app.shared.library.LibraryIndexScheduler
import com.omnidroid.app.shared.library.PendingOperationsMonitor
import com.omnidroid.app.shared.settings.StorageFrameworkPickerLauncher
import com.omnidroid.lib.core.SyncProgress
import com.omnidroid.lib.core.SyncProgressTracker
import com.omnidroid.lib.library.GameSystem
import com.omnidroid.lib.library.MetaSystemID
import com.omnidroid.lib.library.db.RetrogradeDatabase
import com.omnidroid.lib.library.db.dao.GameOrder
import com.omnidroid.lib.library.db.dao.SystemCount
import com.omnidroid.lib.library.db.dao.selectPaged
import com.omnidroid.lib.library.db.entity.Game
import com.omnidroid.lib.library.metaSystemID
import com.omnidroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers

class LibraryViewModel(
    private val appContext: Context,
    private val retrogradeDb: RetrogradeDatabase,
    private val registeredSystemsStore: RegisteredSystemsStore,
    private val startupStore: LibraryStartupStore,
) : ViewModel() {
    class Factory(
        private val appContext: Context,
        private val retrogradeDb: RetrogradeDatabase,
        private val registeredSystemsStore: RegisteredSystemsStore,
        private val startupStore: LibraryStartupStore,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(appContext, retrogradeDb, registeredSystemsStore, startupStore) as T
        }
    }

    data class UiState(
        val filter: LibraryFilter = LibraryFilter.All,
        val searchQuery: String = "",
        val sidebarSystems: List<MetaSystemID> = emptyList(),
        val continueGame: Game? = null,
        val operationInProgress: Boolean = false,
        val syncProgress: SyncProgress? = null,
        val zoomDensity: Int = 0,
        /** Home's view for the current orientation (each orientation remembers its own). */
        val homeView: HomeView = HomeView.CAROUSEL,
        val sort: LibrarySort = LibrarySort.TITLE,
        val selectedSystemGameCount: Int? = null,
        val startupFilter: LibraryFilter = LibraryFilter.All,
    )

    private data class LibraryChrome(
        val operationInProgress: Boolean,
        val syncProgress: SyncProgress?,
        val presentation: LibraryPresentation,
        val systemCounts: List<SystemCount>,
        val startupFilter: LibraryFilter,
    )

    private data class LibraryPresentation(
        val zoomDensity: Int,
        val homeView: HomeView,
        val sort: LibrarySort,
    )

    private val preferences = SharedPreferencesHelper.getSharedPreferences(appContext)

    init {
        migrateLayoutPrefs()
    }

    private val startupFilterFlow = startupStore.observe()
    private val filterFlow = MutableStateFlow(startupStore.get())
    private val searchQueryFlow = MutableStateFlow("")
    private val scanRequested = MutableStateFlow(false)
    private val zoomDensityFlow = MutableStateFlow(readZoomDensity())
    private val portraitFlow =
        MutableStateFlow(appContext.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
    private val landscapeViewFlow = MutableStateFlow(readHomeView(portrait = false))
    private val portraitViewFlow = MutableStateFlow(readHomeView(portrait = true))
    private val homeViewFlow = combine(portraitFlow, landscapeViewFlow, portraitViewFlow, HomeViewPrefs::viewFor)

    // Settings › Display writes these keys too; follow them while the library stays alive.
    private val layoutPreferenceListener =
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                HomeViewPrefs.LANDSCAPE_KEY -> landscapeViewFlow.value = readHomeView(portrait = false)
                HomeViewPrefs.PORTRAIT_KEY -> portraitViewFlow.value = readHomeView(portrait = true)
            }
        }

    init {
        preferences.registerOnSharedPreferenceChangeListener(layoutPreferenceListener)
    }

    override fun onCleared() {
        preferences.unregisterOnSharedPreferenceChangeListener(layoutPreferenceListener)
        super.onCleared()
    }
    private val sortFlow = MutableStateFlow(readSort())
    private val syncProgressFlow = SyncProgressTracker.progress
    private val presentationFlow =
        combine(zoomDensityFlow, homeViewFlow, sortFlow) { zoom, homeView, sort ->
            LibraryPresentation(zoom, homeView, sort)
        }
    private val libraryOperationInProgress =
        PendingOperationsMonitor(appContext).anyLibraryOperationInProgress()

    val state: StateFlow<UiState> =
        combine(
            filterFlow,
            searchQueryFlow,
            sidebarSystems(),
            continueGame(),
            combine(
                combine(
                    scanRequested,
                    libraryOperationInProgress,
                    syncProgressFlow,
                ) { requested, inProgress, progress ->
                    Pair(requested || inProgress, progress)
                },
                presentationFlow,
                retrogradeDb.gameDao().selectSystemsWithCount(),
                startupFilterFlow,
            ) { opStatus, presentation, counts, startup ->
                LibraryChrome(
                    opStatus.first,
                    opStatus.second,
                    presentation,
                    counts,
                    startup,
                )
            },
        ) { filter, searchQuery, sidebarSystems, continueGames, chrome ->
            val selectedCount =
                (filter as? LibraryFilter.System)?.let { system ->
                    val ids = system.metaSystemID.systemIDs.map { it.dbname }.toSet()
                    chrome.systemCounts.filter { it.systemId in ids }.sumOf { it.count }
                }
            UiState(
                filter = filter,
                searchQuery = searchQuery,
                sidebarSystems = sidebarSystems,
                continueGame = continueGames.firstOrNull(),
                operationInProgress = chrome.operationInProgress,
                syncProgress = if (chrome.operationInProgress) chrome.syncProgress else null,
                zoomDensity = chrome.presentation.zoomDensity,
                homeView = chrome.presentation.homeView,
                sort = chrome.presentation.sort,
                selectedSystemGameCount = selectedCount,
                startupFilter = chrome.startupFilter,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UiState(
                zoomDensity = zoomDensityFlow.value,
                homeView = currentHomeView(),
                sort = sortFlow.value,
                filter = filterFlow.value,
                startupFilter = startupStore.get(),
            ),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val games: Flow<PagingData<Game>> =
        combine(filterFlow, searchQueryFlow, sortFlow) { filter, query, sort -> Triple(filter, query.trim(), sort) }
            .flatMapLatest { (filter, query, sort) ->
                Pager(LibraryPaging.config()) {
                    gamesPagingSource(filter, query, sort)
                }.flow
            }.cachedIn(viewModelScope)

    init {
        // Leave the startup view if the user changes it while already looking at it.
        viewModelScope.launch {
            var previous = startupStore.get()
            startupFilterFlow.collect { startup ->
                if (startup != previous) {
                    if (filterFlow.value == previous) {
                        filterFlow.value = LibraryFilter.All
                    }
                    previous = startup
                }
            }
        }
    }

    fun selectFilter(filter: LibraryFilter) {
        filterFlow.value = filter
    }

    fun setStartupFilter(filter: LibraryFilter) {
        startupStore.set(filter)
    }

    fun cycleFilter(delta: Int) {
        val systems = state.value.sidebarSystems
        val items =
            buildList {
                add(LibraryFilter.Favorites)
                add(LibraryFilter.All)
                systems.forEach { add(LibraryFilter.System(it)) }
            }
        if (items.isEmpty()) return
        val current = items.indexOfFirst { it == state.value.filter }.let { index ->
            if (index < 0) 1 else index
        }
        selectFilter(items[(current + delta).mod(items.size)])
    }

    fun changeQueryString(query: String) {
        searchQueryFlow.value = query
    }

    fun clearSearchQuery() {
        searchQueryFlow.value = ""
    }

    fun setZoomDensity(density: Int) {
        val value = density.coerceIn(0, MAX_ZOOM_DENSITY)
        zoomDensityFlow.value = value
        preferences.edit().putInt(ZOOM_DENSITY_KEY, value).apply()
    }

    /** The screen's orientation; Home shows the view chosen for it. */
    fun setPortrait(portrait: Boolean) {
        portraitFlow.value = portrait
    }

    /** Changes Home's view for the current orientation only. */
    fun setHomeView(view: HomeView) {
        val portrait = portraitFlow.value
        (if (portrait) portraitViewFlow else landscapeViewFlow).value = view
        preferences.edit().putString(HomeViewPrefs.key(portrait), view.name).apply()
    }

    private fun currentHomeView(): HomeView =
        HomeViewPrefs.viewFor(portraitFlow.value, landscapeViewFlow.value, portraitViewFlow.value)

    private fun readHomeView(portrait: Boolean): HomeView =
        HomeView.fromName(preferences.getString(HomeViewPrefs.key(portrait), null)) ?: HomeView.CAROUSEL

    /**
     * Once: Home became the only layout and each orientation got its own view. Carry over what
     * the user saw before into both, then drop the old keys.
     */
    private fun migrateLayoutPrefs() {
        if (preferences.contains(HomeViewPrefs.LANDSCAPE_KEY) || preferences.contains(HomeViewPrefs.PORTRAIT_KEY)) return
        val view =
            HomeViewPrefs.migrate(
                preferences.getString(HomeViewPrefs.OLD_LAYOUT_KEY, null),
                preferences.getString(HomeViewPrefs.OLD_HOME_VIEW_KEY, null),
            )
        preferences.edit()
            .putString(HomeViewPrefs.LANDSCAPE_KEY, view.name)
            .putString(HomeViewPrefs.PORTRAIT_KEY, view.name)
            .remove(HomeViewPrefs.OLD_LAYOUT_KEY)
            .remove(HomeViewPrefs.OLD_HOME_VIEW_KEY)
            .remove(HomeViewPrefs.OLD_CLASSIC_LAYOUT_KEY)
            .apply()
    }

    private fun readZoomDensity(): Int {
        return preferences.getInt(ZOOM_DENSITY_KEY, 0).coerceIn(0, MAX_ZOOM_DENSITY)
    }

    fun setSort(sort: LibrarySort) {
        sortFlow.value = sort
        preferences.edit().putString(SORT_KEY, sort.name).apply()
    }

    private fun readSort(): LibrarySort {
        val saved = preferences.getString(SORT_KEY, null)
        return LibrarySort.entries.firstOrNull { it.name == saved } ?: LibrarySort.TITLE
    }

    /** A random game from the current console or favorites filter, ignoring the search. */
    suspend fun randomGame(): Game? {
        val dao = retrogradeDb.gameDao()
        return when (val filter = filterFlow.value) {
            is LibraryFilter.Favorites -> dao.selectRandomFavorite()
            is LibraryFilter.System -> dao.selectRandomBySystems(filter.systemIds())
            else -> dao.selectRandom()
        }
    }

    /** Reconcile folder changes on resume / watcher without prompting for a folder. */
    fun autoSyncLibrary(context: Context) {
        val folderKey = context.getString(com.omnidroid.lib.R.string.pref_key_extenral_folder)
        val folder =
            SharedPreferencesHelper.getLegacySharedPreferences(context)
                .getString(folderKey, null)
        if (folder.isNullOrBlank()) return
        syncLibrary(context, forceDeep = false)
    }

    suspend fun clearMissingGames(): Int =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val missing = retrogradeDb.gameDao().selectUnavailable()
            missing.forEach { game ->
                game.customCoverPath?.let { path ->
                    runCatching { java.io.File(path.substringBefore('?')).delete() }
                }
            }
            if (missing.isNotEmpty()) {
                retrogradeDb.gameDao().delete(missing)
            }
            missing.size
        }

    fun zoomIn() {
        if (currentHomeView() != HomeView.GRID) return
        setZoomDensity(zoomDensityFlow.value - 1)
    }

    fun zoomOut() {
        if (currentHomeView() != HomeView.GRID) return
        setZoomDensity(zoomDensityFlow.value + 1)
    }

    fun syncLibrary(
        context: Context,
        forceDeep: Boolean = true,
    ) {
        if (scanRequested.value || state.value.operationInProgress) return

        val folderKey = context.getString(com.omnidroid.lib.R.string.pref_key_extenral_folder)
        val folder =
            SharedPreferencesHelper.getLegacySharedPreferences(context)
                .getString(folderKey, null)

        if (folder.isNullOrBlank()) {
            StorageFrameworkPickerLauncher.pickFolder(context)
            return
        }

        viewModelScope.launch {
            scanRequested.value = true
            try {
                LibraryIndexScheduler.scheduleLibrarySync(
                    context.applicationContext,
                    forceDeep = forceDeep,
                )
                val started = withTimeoutOrNull(3_000) { libraryOperationInProgress.first { it } }
                if (started == true) {
                    libraryOperationInProgress.first { !it }
                }
            } finally {
                scanRequested.value = false
            }
        }
    }

    private fun sidebarSystems(): Flow<List<MetaSystemID>> {
        return combine(
            registeredSystemsStore.observe(),
            retrogradeDb.gameDao().selectSystemsWithCount(),
            startupFilterFlow,
        ) { registered, counts, startup ->
            val withGames =
                counts.mapNotNull { count ->
                    if (count.count <= 0) return@mapNotNull null
                    runCatching { GameSystem.findById(count.systemId).metaSystemID() }.getOrNull()
                }.toSet()

            (registered + withGames + listOfNotNull((startup as? LibraryFilter.System)?.metaSystemID))
                .distinct()
                .sortedBy { appContext.getString(it.titleResId) }
        }
    }

    private fun continueGame(): Flow<List<Game>> = retrogradeDb.gameDao().selectLastPlayed()

    private fun gamesPagingSource(
        filter: LibraryFilter,
        query: String,
        sort: LibrarySort,
    ) = retrogradeDb.gameDao().selectPaged(
        systemIds = (filter as? LibraryFilter.System)?.systemIds(),
        favoritesOnly = filter is LibraryFilter.Favorites,
        query = query,
        order = sort.order,
        systemOrder = consoleOrder,
    )

    /** System ids in the dock's console order. */
    private val consoleOrder by lazy {
        MetaSystemID.entries
            .sortedBy { appContext.getString(it.titleResId) }
            .flatMap { meta -> meta.systemIDs.map { it.dbname } }
    }

    private fun LibraryFilter.System.systemIds() = metaSystemID.systemIDs.map { it.dbname }

    companion object {
        private const val ZOOM_DENSITY_KEY = "pref_library_zoom_density"
        private const val SORT_KEY = "pref_library_sort"
        const val MAX_ZOOM_DENSITY = 2
    }
}

enum class LibrarySort(val order: GameOrder) {
    TITLE(GameOrder.TITLE),
    CONSOLE(GameOrder.CONSOLE),
    RECENTLY_PLAYED(GameOrder.RECENTLY_PLAYED),
    FAVORITES_FIRST(GameOrder.FAVORITES_FIRST),
}
