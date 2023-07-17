package com.gildedrose

import com.github.quillraven.fleks.*
import com.github.quillraven.fleks.World.Companion.family

class GildedRose(
    var items: List<Item>
) {
    val world = configureWorld {

        components {
            onAdd(Box2dComponent) { entity, b2dCmp ->

            }
            onRemove(Box2dComponent) { entity, b2dCmp ->

            }
        }

        families {
            val moveFamily = family { all(MoveComponent) }
            onAdd(moveFamily) { entity ->

            }
            onRemove(moveFamily) { entity ->

            }
        }

        systems {
            add(GildedRoseSystem())
        }

        onAddEntity { entity ->

        }

        onRemoveEntity { entity ->

        }
    }

    fun updateQuality() {
        val entities = items.map { item ->
            world.entity { entity ->
                entity += Freshness(item.sellIn)
                entity += Quality(item.quality)

                entity += when {
                    item.name.startsWith("Conjured")          -> ItemType.Conjured
                    item.name.startsWith("Backstage passes")  -> ItemType.BackstagePass
                    item.name == "Sulfuras, Hand of Ragnaros" -> ItemType.Legendary
                    item.name == "Aged Brie"                  -> ItemType.Aged
                    else                                      -> ItemType.Regular
                }
            }
        }

        world.update(1f)

//        val items = items.map(::ImmutableItem)

        for (item in items) {
            if (item.name != "Aged Brie" && item.name != "Backstage passes to a TAFKAL80ETC concert") {
                if (item.quality > 0) {
                    if (item.name != "Sulfuras, Hand of Ragnaros") {
                        item.quality = item.quality - 1
                    }
                }
            } else {
                if (item.quality < 50) {
                    item.quality = item.quality + 1

                    if (item.name == "Backstage passes to a TAFKAL80ETC concert") {
                        if (item.sellIn < 11) {
                            if (item.quality < 50) {
                                item.quality = item.quality + 1
                            }
                        }

                        if (item.sellIn < 6) {
                            if (item.quality < 50) {
                                item.quality = item.quality + 1
                            }
                        }
                    }
                }
            }

            if (item.name != "Sulfuras, Hand of Ragnaros") {
                item.sellIn = item.sellIn - 1
            }

            if (item.sellIn < 0) {
                if (item.name != "Aged Brie") {
                    if (item.name != "Backstage passes to a TAFKAL80ETC concert") {
                        if (item.quality > 0) {
                            if (item.name != "Sulfuras, Hand of Ragnaros") {
                                item.quality = item.quality - 1
                            }
                        }
                    } else {
                        item.quality = 0
                    }
                } else {
                    if (item.quality < 50) {
                        item.quality = item.quality + 1
                    }
                }
            }
        }
    }

}


private class FreshnessSystem : IteratingSystem(
    family { all(ItemComponent) }
) {
    override fun onTickEntity(entity: Entity) {
        val component = entity[Freshness]
        component.item.quality += if (component.item.isExpired()) 2 else 1
    }
}

private class AgedItemSystem : IteratingSystem(
    family { all(ItemComponent.Aged) }
) {
    override fun onTickEntity(entity: Entity) {
        val component = entity[ItemComponent.Aged]
        component.item.quality += if (component.item.isExpired()) 2 else 1
    }
}

///**
// * @param[sellInDays] denotes the number of days we have to sell the item
// */
//private data class ImmutableItem(
//    val name: String,
//    val sellInDays: Int,
//    val quality: Int,
//) {
//    constructor(item: Item) : this(item.name, item.sellIn, item.quality)
//
//    fun toItem(): Item = Item(name, sellInDays, quality)
//}


//@JvmInline
//private value class ItemComponent(val item: Item) : Component<ItemComponent> {
//    override fun type(): ComponentType<ItemComponent> = ItemComponent
//
//    companion object : ComponentType<ItemComponent>()
//}

private sealed interface ItemComponent<T : ItemComponent<T>> : Component<T> {
    val item: Item

    override fun type(): ComponentType<T> = ItemComponent.Companion

    /** > "Aged Brie" actually increases in Quality the older it gets */
    class Aged(override val item: Item) : ItemComponent<Aged> {
        companion object : ComponentType<Aged>()
    }

    /** > "Conjured" items degrade in Quality twice as fast as normal items */
    class Conjured(override val item: Item) : ItemComponent<Conjured> {
        override fun type(): ComponentType<Conjured> = ItemComponent

        companion object : ComponentType<Conjured>()
    }

    /** > "Sulfuras", being a legendary item, never has to be sold or decreases in Quality */
    class Legendary(override val item: Item) : ItemComponent<Legendary> {
        override fun type(): ComponentType<Legendary> = Legendary

        companion object : ComponentType<Legendary>()
    }

    /** > "Once the sell by date has passed, Quality degrades twice as fast" */
    class Regular(override val item: Item) : ItemComponent<Regular> {
        override fun type(): ComponentType<Regular> = Regular

        companion object : ComponentType<Regular>()
    }

    /**
     * > "Backstage passes", like aged brie, increases in Quality as its SellIn value approaches;
     * >
     * > Quality
     * > * increases by 2 when there are 10 days or less
     * > * and by 3 when there are 5 days or less
     * > * but drops to 0 after the concert
     */
    class BackstagePass(override val item: Item) : ItemComponent<BackstagePass> {
        override fun type(): ComponentType<BackstagePass> = BackstagePass

        companion object : ComponentType<BackstagePass>()
    }


    companion object : ComponentType<ItemComponent<*>>()
}

private data class Quality(var value: Int) : Component<Quality> {
    override fun type(): ComponentType<Quality> = Quality

    companion object : ComponentType<Quality>()
}

private data class Freshness(var value: Int) : Component<Freshness> {
    override fun type(): ComponentType<Freshness> = Freshness

    companion object : ComponentType<Freshness>()
}
