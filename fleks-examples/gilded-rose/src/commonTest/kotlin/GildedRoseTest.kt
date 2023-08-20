package com.gildedrose

import kotlin.test.Test
import kotlin.test.assertEquals


internal class GildedRoseTest {

    @Test
    fun foo() {
        val items = listOf(Item("foo", 0, 0))
        val app = GildedRose(items)
        app.updateQuality()
        assertEquals("fixme", app.items[0].name)
    }

}
