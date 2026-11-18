package com.omnidroid.app.mobile.feature.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryLayoutTest {
    @Test
    fun `saved names, with Console for missing or unknown ones`() {
        assertEquals(LibraryLayout.GRID, LibraryLayout.fromName("GRID"))
        assertEquals(LibraryLayout.LIST, LibraryLayout.fromName("LIST"))
        assertEquals(LibraryLayout.CONSOLE, LibraryLayout.fromName("CONSOLE"))
        assertEquals(LibraryLayout.CONSOLE, LibraryLayout.fromName(null))
        assertEquals(LibraryLayout.CONSOLE, LibraryLayout.fromName("HOME"))
    }

    @Test
    fun `each orientation shows its own layout`() {
        assertEquals(LibraryLayout.CONSOLE, LibraryLayout.forOrientation(false, LibraryLayout.CONSOLE, LibraryLayout.GRID))
        assertEquals(LibraryLayout.GRID, LibraryLayout.forOrientation(true, LibraryLayout.CONSOLE, LibraryLayout.GRID))
    }

    @Test
    fun `landscape keeps the original key`() {
        assertEquals("pref_library_layout", LibraryViewModel.layoutKey(portrait = false))
        assertEquals("pref_library_layout_portrait", LibraryViewModel.layoutKey(portrait = true))
    }
}
