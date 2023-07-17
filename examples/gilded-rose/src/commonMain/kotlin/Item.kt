package com.gildedrose

import com.gildedrose.ItemMutator.Companion.constantQualityMutator
import com.gildedrose.ItemMutator.Companion.mutatorSelector

/**
 * @param[sellIn] All items have a `SellIn` value which denotes the number of days we have to sell the item
 */
open class Item(var name: String, var sellIn: Int, var quality: Int) {
    override fun toString(): String {
        return this.name + ", " + this.sellIn + ", " + this.quality
    }
}



object ItemTemplates {

    /** Decrease sellIn by `-1` */
    val regularSellInDecrement = ItemMutator { it.copy(sellIn = it.sellIn - 1) }

    /** > "Sulfuras", being a legendary item, never has to be sold or decreases in Quality */
    val legendaryItem = ItemTemplate(
        constantQualityMutator(80),
    )

    /** Regular, non-legendary, items have a valid range of 0 to 50 (inclusive). */
    val regularItemValidRange = 0..50

    /**
     * Create a [ItemTemplate] for an [Item]. The resulting template:
     *
     *  - adjusts [Item.quality] using the delta from [qualityDeltaProvider]
     *  - adjust [Item.sellIn] using [regularSellInDecrement]
     */
    fun qualityAdjustingItemTemplate(
        qualityDeltaProvider: (ItemState) -> Int,
    ) = ItemTemplate(
        ItemMutator.QualityMutator(regularItemValidRange, qualityDeltaProvider),
        regularSellInDecrement,
    )

    /** > "Once the sell by date has passed, Quality degrades twice as fast" */
    val regularItem = qualityAdjustingItemTemplate { item ->
        when {
            item.isExpired -> -2
            else           -> -1
        }
    }

    /** > "Aged Brie" actually increases in Quality the older it gets */
    val agedItem = qualityAdjustingItemTemplate { item ->
        when {
            item.isExpired -> 2
            else           -> 1
        }
    }

    /** > "Conjured" items degrade in Quality twice as fast as normal items */
    val conjuredItem = qualityAdjustingItemTemplate { item ->
        when {
            item.isExpired -> -4
            else           -> -2
        }
    }

    /**
     * > "Backstage passes", like aged brie, increases in Quality as its SellIn value approaches;
     * >
     * > Quality
     * > * increases by 2 when there are 10 days or less
     * > * and by 3 when there are 5 days or less
     * > * but drops to 0 after the concert
     *
     * Uses a [mutatorSelector] to set quality to 0 if the item is expired, else it will mutate
     * using a [ItemMutator.QualityMutator].
     */
    val ticketItem = run {

        val expiredMutator = constantQualityMutator(0)

        val nonExpiredMutator = ItemMutator.QualityMutator(regularItemValidRange) { item ->
            require(!item.isExpired) { "expired tickets should be handled by 'expiredMutator'" }
            when {
                // "increases by 3 when there are 5 days or less"
                item.sellIn <= 5  -> 3
                // "increases by 2 when there are 10 days or less"
                item.sellIn <= 10 -> 2
                // "increases in Quality as its SellIn approaches"
                else              -> 1
            }
        }

        // select the expired/non-expired mutator based on Item expiration
        val ticketMutator = mutatorSelector { item ->
            when {
                item.isExpired -> expiredMutator
                else           -> nonExpiredMutator
            }
        }

        // combine the quality mutator with a regular sellIn decrement
        ItemTemplate(ticketMutator + regularSellInDecrement)
    }
}


/**
 * Given an [ItemState], mutate it and return a new [ItemState].
 *
 * See [ItemTemplates] for implementations.
 *
 * Multiple mutators can be combined using [ItemMutator.plus].
 *
 * @see ItemTemplates
 */
fun interface ItemMutator {

    fun applyMutation(item: ItemState): ItemState

    /** Create a new [ItemMutator] that first applies this mutation, and then [other]. */
    operator fun plus(other: ItemMutator): ItemMutator =
        ItemMutator { item -> other.applyMutation(applyMutation(item)) }

    /** Returns a new [ItemState] with an adjusted [ItemState.quality]. */
    class QualityMutator(
        private val range: IntRange,
        val qualityDeltaProvider: (ItemState) -> Int
    ) : ItemMutator {

        override fun applyMutation(item: ItemState): ItemState {
            val delta = qualityDeltaProvider(item)
            val newQuality = (item.quality + delta).coerceIn(range)
            return item.copy(quality = newQuality)
        }

    }

    companion object {

        /** Sets the quality to be a constant number, regardless of the current state. */
        fun constantQualityMutator(constantQuality: Int) =
            ItemMutator { it.copy(quality = constantQuality) }

        /** Select between different [ItemMutator]s, based on an [ItemState]. */
        fun mutatorSelector(selector: (ItemState) -> ItemMutator) =
            ItemMutator { item -> selector(item).applyMutation(item) }

    }
}

/**
 * Freeze the state of an [Item], so it can be used safely in other contexts without mutating
 * the original.
 */
data class ItemState(
    val name: String,
    val sellIn: Int,
    val quality: Int,
    val isExpired: Boolean,
) {

    constructor(item: Item) : this(item.name, item.sellIn, item.quality, item.isExpired())

    companion object {
        fun Item.asState(): ItemState = ItemState(this)
    }
}


fun Item.isExpired(): Boolean = sellIn <= 0


/**
 * Provides an [ItemTemplate] for a given [ItemState].
 */
fun interface ItemTemplateProvider {

    fun getTemplate(item: ItemState): ItemTemplate

    companion object {

        val defaultTemplateProvider = ItemTemplateProvider {
            it.name.run {
                when {
                    // conjured
                    startsWith("Conjured")                              -> ItemTemplates.conjuredItem
                    // tickets
                    equals("Backstage passes to a TAFKAL80ETC concert") -> ItemTemplates.ticketItem
                    // legendary
                    equals("Sulfuras, Hand of Ragnaros")                -> ItemTemplates.legendaryItem
                    // aged
                    equals("Aged Brie")                                 -> ItemTemplates.agedItem
                    // regular
                    else                                                -> ItemTemplates.regularItem
                }
            }
        }
    }
}

/**
 * Defines how a specific instance of an [ItemState] should be handled.
 *
 * Holds functions for the mutation of [ItemState]s.
 *
 * A template can be associated with a template with [ItemTemplateProvider].
 *
 * See [ItemTemplates] for implementations.
 *
 * @see ItemTemplates
 */
open class ItemTemplate(
    private val mutator: ItemMutator,
) : ItemMutator by mutator {

    constructor(
        vararg mutators: ItemMutator
    ) : this(
        if (mutators.isEmpty())
            throw IllegalStateException("At least one mutator is required, received none.")
        else
            mutators.asSequence().reduce(ItemMutator::plus)
    )

}
