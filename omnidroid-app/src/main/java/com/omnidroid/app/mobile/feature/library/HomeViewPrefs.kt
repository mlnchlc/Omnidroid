package com.omnidroid.app.mobile.feature.library

/** The views Home offers, each shown with the consoles along the bottom. */
enum class HomeView {
    CAROUSEL,
    GRID,
    LIST,
    ;

    companion object {
        /** A saved name; "ROW" is what the carousel was called before. */
        fun fromName(name: String?): HomeView? = if (name == "ROW") CAROUSEL else entries.firstOrNull { it.name == name }
    }
}

/**
 * Pref keys for the Home view, one per orientation, and the move from the older layout prefs.
 * Kept free of Android so it can be unit tested.
 */
object HomeViewPrefs {
    const val LANDSCAPE_KEY = "pref_library_home_view_landscape"
    const val PORTRAIT_KEY = "pref_library_home_view_portrait"

    /** Before: one library layout (Console, Home, Grid, List) and Home's own view. */
    const val OLD_LAYOUT_KEY = "pref_library_layout"
    const val OLD_HOME_VIEW_KEY = "pref_library_home_view"
    const val OLD_CLASSIC_LAYOUT_KEY = "pref_library_classic_layout"

    fun key(portrait: Boolean): String = if (portrait) PORTRAIT_KEY else LANDSCAPE_KEY

    /**
     * The Home view matching what someone saw before Home became the only layout: Home's own view
     * if they were on Home, Grid or List if they used those, else the carousel (Console's
     * successor, and the default).
     */
    fun migrate(
        oldLayout: String?,
        oldHomeView: String?,
    ): HomeView =
        when (oldLayout) {
            "HOME" -> HomeView.fromName(oldHomeView) ?: HomeView.CAROUSEL
            "GRID" -> HomeView.GRID
            "LIST" -> HomeView.LIST
            else -> HomeView.CAROUSEL
        }

    fun viewFor(
        portrait: Boolean,
        landscapeView: HomeView,
        portraitView: HomeView,
    ): HomeView = if (portrait) portraitView else landscapeView
}
