package com.omnidroid.app.mobile.feature.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeViewPrefsTest {
    @Test
    fun `old Home users keep their Home view, with Row now the carousel`() {
        assertEquals(HomeView.CAROUSEL, HomeViewPrefs.migrate("HOME", "ROW"))
        assertEquals(HomeView.GRID, HomeViewPrefs.migrate("HOME", "GRID"))
        assertEquals(HomeView.LIST, HomeViewPrefs.migrate("HOME", "LIST"))
        assertEquals(HomeView.CAROUSEL, HomeViewPrefs.migrate("HOME", null))
    }

    @Test
    fun `old Grid and List users get the same view on Home`() {
        assertEquals(HomeView.GRID, HomeViewPrefs.migrate("GRID", "ROW"))
        assertEquals(HomeView.LIST, HomeViewPrefs.migrate("LIST", null))
    }

    @Test
    fun `old Console users, fresh installs and junk get the carousel`() {
        assertEquals(HomeView.CAROUSEL, HomeViewPrefs.migrate("CONSOLE", "LIST"))
        assertEquals(HomeView.CAROUSEL, HomeViewPrefs.migrate(null, null))
        assertEquals(HomeView.CAROUSEL, HomeViewPrefs.migrate("SIDEBAR", "???"))
    }

    @Test
    fun `saved names`() {
        assertEquals(HomeView.CAROUSEL, HomeView.fromName("ROW"))
        assertEquals(HomeView.GRID, HomeView.fromName("GRID"))
        assertNull(HomeView.fromName("CONSOLE"))
        assertNull(HomeView.fromName(null))
    }

    @Test
    fun `each orientation shows its own view`() {
        assertEquals(HomeView.CAROUSEL, HomeViewPrefs.viewFor(portrait = false, landscapeView = HomeView.CAROUSEL, portraitView = HomeView.GRID))
        assertEquals(HomeView.GRID, HomeViewPrefs.viewFor(portrait = true, landscapeView = HomeView.CAROUSEL, portraitView = HomeView.GRID))
        assertEquals("pref_library_home_view_portrait", HomeViewPrefs.key(portrait = true))
        assertEquals("pref_library_home_view_landscape", HomeViewPrefs.key(portrait = false))
    }
}
