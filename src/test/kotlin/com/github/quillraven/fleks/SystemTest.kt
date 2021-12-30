package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.compareEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame

private class SystemTestIntervalSystemEachFrame : IntervalSystem(
    interval = EachFrame
) {
    var numDisposes = 0
    var numCalls = 0

    override fun onTick() {
        ++numCalls
    }

    override fun onDispose() {
        numDisposes++
    }
}

private class SystemTestIntervalSystemFixed : IntervalSystem(
    interval = Fixed(0.25f)
) {
    var numCalls = 0
    var lastAlpha = 0f

    override fun onTick() {
        ++numCalls
    }

    override fun onAlpha(alpha: Float) {
        lastAlpha = alpha
    }
}

private class SystemTestIntervalSystemMultipleCstrs(
    val arg: String = ""
) : IntervalSystem() {
    override fun onTick() = Unit
}

private data class SystemTestComponent(var x: Float = 0f)

private class SystemTestIteratingSystemNoFamily : IteratingSystem() {
    override fun onTickEntity(entity: Entity) = Unit
}

@AllOf([SystemTestComponent::class])
private class SystemTestIteratingSystemMapper(
    val mapper: ComponentMapper<SystemTestComponent>
) : IteratingSystem(interval = Fixed(0.25f)) {
    var numEntityCalls = 0
    var numAlphaCalls = 0
    var lastAlpha = 0f
    var entityToConfigure: Entity? = null

    override fun onTickEntity(entity: Entity) {
        entityToConfigure?.let { e ->
            configureEntity(e) {
                mapper.remove(it)
            }
        }
        ++numEntityCalls
    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        lastAlpha = alpha
        ++numAlphaCalls
    }
}

@AllOf([SystemTestComponent::class])
private class SystemTestIteratingSystemSortAutomatic(
    val mapper: ComponentMapper<SystemTestComponent>
) : IteratingSystem(
    compareEntity { e1, e2 -> mapper[e2].x.compareTo(mapper[e1].x) }
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)
    var entityToRemove: Entity? = null

    override fun onTickEntity(entity: Entity) {
        entityToRemove?.let {
            world.remove(it)
            entityToRemove = null
        }

        lastEntityProcess = entity
        ++numEntityCalls
    }
}

@AllOf([SystemTestComponent::class])
private class SystemTestIteratingSystemSortManual(
    val mapper: ComponentMapper<SystemTestComponent>
) : IteratingSystem(
    compareEntity { e1, e2 -> mapper[e2].x.compareTo(mapper[e1].x) },
    sortingType = Manual
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)

    override fun onTickEntity(entity: Entity) {
        lastEntityProcess = entity
        ++numEntityCalls
    }
}

@NoneOf([SystemTestComponent::class])
@AnyOf([SystemTestComponent::class])
private class SystemTestIteratingSystemInjectable(
    val injectable: String
) : IteratingSystem() {
    override fun onTickEntity(entity: Entity) = Unit
}

internal class SystemTest {
    @Test
    fun `system with interval EachFrame gets called every time`() {
        val system = SystemTestIntervalSystemEachFrame()

        system.onUpdate()
        system.onUpdate()

        assertEquals(2, system.numCalls)
    }

    @Test
    fun `system with interval EachFrame returns world's delta time`() {
        val system = SystemTestIntervalSystemEachFrame()
        system.world.update(42f)

        assertEquals(42f, system.deltaTime)
    }

    @Test
    fun `system with fixed interval of 0,25f gets called four times when delta time is 1,1f`() {
        val system = SystemTestIntervalSystemFixed()
        system.world.update(1.1f)

        system.onUpdate()

        assertAll(
            { assertEquals(4, system.numCalls) },
            { assertEquals(0.1f / 0.25f, system.lastAlpha, 0.0001f) }
        )
    }

    @Test
    fun `system with fixed interval returns step rate as delta time`() {
        val system = SystemTestIntervalSystemFixed()

        assertEquals(0.25f, system.deltaTime, 0.0001f)
    }

    @Test
    fun `create IntervalSystem with no-args`() {
        val expectedWorld = World {}

        val service = SystemService(
            expectedWorld,
            listOf(SystemTestIntervalSystemEachFrame::class),
            emptyMap()
        )

        assertAll(
            { assertEquals(1, service.systems.size) },
            { assertNotNull(service.system<SystemTestIntervalSystemEachFrame>()) },
            { assertSame(expectedWorld, service.system<SystemTestIntervalSystemEachFrame>().world) }
        )
    }

    @Test
    fun `create IteratingSystem with a ComponentMapper arg`() {
        val expectedWorld = World {}

        val service = SystemService(
            expectedWorld,
            listOf(SystemTestIteratingSystemMapper::class),
            emptyMap()
        )

        val actualSystem = service.system<SystemTestIteratingSystemMapper>()
        assertAll(
            { assertEquals(1, service.systems.size) },
            { assertSame(expectedWorld, actualSystem.world) },
            { assertEquals(SystemTestComponent::class.java.name, actualSystem.mapper.cstr.name) },
        )
    }

    @Test
    fun `create IteratingSystem with an injectable arg`() {
        val expectedWorld = World {}

        val service = SystemService(
            expectedWorld,
            listOf(SystemTestIteratingSystemInjectable::class),
            mapOf(String::class to "42")
        )

        val actualSystem = service.system<SystemTestIteratingSystemInjectable>()
        assertAll(
            { assertEquals(1, service.systems.size) },
            { assertSame(expectedWorld, actualSystem.world) },
            { assertEquals("42", actualSystem.injectable) },
        )
    }

    @Test
    fun `cannot create IteratingSystem without family annotations`() {
        assertThrows<FleksSystemCreationException> {
            SystemService(
                World.EMPTY_WORLD,
                listOf(SystemTestIteratingSystemNoFamily::class),
                emptyMap()
            )
        }
    }

    @Test
    fun `cannot create IteratingSystem with missing injectables`() {
        assertThrows<FleksSystemCreationException> {
            SystemService(
                World.EMPTY_WORLD,
                listOf(SystemTestIteratingSystemInjectable::class),
                emptyMap()
            )
        }
    }

    @Test
    fun `cannot create system with multiple constructors`() {
        assertThrows<FleksSystemCreationException> {
            SystemService(
                World.EMPTY_WORLD,
                listOf(SystemTestIntervalSystemMultipleCstrs::class),
                emptyMap()
            )
        }
    }

    @Test
    fun `IteratingSystem calls onTick and onAlpha for each entity of the system`() {
        val world = World {}
        val service = SystemService(
            world,
            listOf(SystemTestIteratingSystemMapper::class),
            emptyMap()
        )
        world.entity { add<SystemTestComponent>() }
        world.entity { add<SystemTestComponent>() }
        world.update(0.3f)

        service.update()

        val system = service.system<SystemTestIteratingSystemMapper>()
        assertAll(
            { assertEquals(2, system.numEntityCalls) },
            { assertEquals(2, system.numAlphaCalls) },
            { assertEquals(0.05f / 0.25f, system.lastAlpha, 0.0001f) }
        )
    }

    @Test
    fun `configure entity during iteration`() {
        val world = World {}
        val service = SystemService(
            world,
            listOf(SystemTestIteratingSystemMapper::class),
            emptyMap()
        )
        world.update(0.3f)
        val entity = world.entity { add<SystemTestComponent>() }
        val system = service.system<SystemTestIteratingSystemMapper>()
        system.entityToConfigure = entity

        service.update()

        assertFalse { entity in system.mapper }
    }

    @Test
    fun `sort entities automatically`() {
        val world = World {}
        val service = SystemService(
            world,
            listOf(SystemTestIteratingSystemSortAutomatic::class),
            emptyMap()
        )
        world.entity { add<SystemTestComponent> { x = 15f } }
        world.entity { add<SystemTestComponent> { x = 10f } }
        val expectedEntity = world.entity { add<SystemTestComponent> { x = 5f } }

        service.update()

        assertEquals(expectedEntity, service.system<SystemTestIteratingSystemSortAutomatic>().lastEntityProcess)
    }

    @Test
    fun `sort entities programmatically`() {
        val world = World {}
        val service = SystemService(
            world,
            listOf(SystemTestIteratingSystemSortManual::class),
            emptyMap()
        )
        world.entity { add<SystemTestComponent> { x = 15f } }
        world.entity { add<SystemTestComponent> { x = 10f } }
        val expectedEntity = world.entity { add<SystemTestComponent> { x = 5f } }
        val system = service.system<SystemTestIteratingSystemSortManual>()

        system.doSort = true
        service.update()

        assertAll(
            { assertEquals(expectedEntity, system.lastEntityProcess) },
            { assertFalse(system.doSort) }
        )
    }

    @Test
    fun `cannot get a non-existing system`() {
        val service = SystemService(
            World.EMPTY_WORLD,
            listOf(SystemTestIteratingSystemSortAutomatic::class),
            emptyMap()
        )

        assertThrows<FleksNoSuchSystemException> {
            service.system<SystemTestIntervalSystemEachFrame>()
        }
    }

    @Test
    fun `update only calls enabled systems`() {
        val service = SystemService(
            World.EMPTY_WORLD,
            listOf(SystemTestIntervalSystemEachFrame::class),
            emptyMap()
        )
        val system = service.system<SystemTestIntervalSystemEachFrame>()
        system.enabled = false

        service.update()

        assertEquals(0, system.numCalls)
    }

    @Test
    fun `removing an entity during iteration is delayed`() {
        val world = World {}
        val service = SystemService(
            world,
            listOf(SystemTestIteratingSystemSortAutomatic::class),
            emptyMap()
        )
        world.entity { add<SystemTestComponent> { x = 15f } }
        val entityToRemove = world.entity { add<SystemTestComponent> { x = 10f } }
        world.entity { add<SystemTestComponent> { x = 5f } }
        val system = service.system<SystemTestIteratingSystemSortAutomatic>()
        system.entityToRemove = entityToRemove

        // call it twice - first call still iterates over all three entities
        // while the second call will only iterate over the remaining two entities
        service.update()
        service.update()

        assertEquals(5, system.numEntityCalls)
    }

    @Test
    fun `dispose service`() {
        val service = SystemService(
            World.EMPTY_WORLD,
            listOf(SystemTestIntervalSystemEachFrame::class),
            emptyMap()
        )

        service.dispose()

        assertEquals(1, service.system<SystemTestIntervalSystemEachFrame>().numDisposes)
    }
}
