package com.arfipod.wearosplayground.examples

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleKindTest {
    @Test
    fun routesResolveKnownExamples() {
        assertEquals(ExampleKind.FLAPPY, ExampleKind.fromRoute("flappy-bird"))
        assertEquals(ExampleKind.API, ExampleKind.fromRoute("network"))
        assertEquals(ExampleKind.METRO, ExampleKind.fromRoute("metro-madrid"))
        assertEquals(ExampleKind.EMT, ExampleKind.fromRoute("e3"))
        assertEquals(ExampleKind.CUBE_3D, ExampleKind.fromRoute("cube_3d"))
        assertEquals(ExampleKind.AUDIO, ExampleKind.fromRoute("sound"))
        assertEquals(ExampleKind.VIDEO, ExampleKind.fromRoute("movie"))
    }

    @Test
    fun unknownRouteReturnsNull() {
        assertNull(ExampleKind.fromRoute(null))
        assertNull(ExampleKind.fromRoute("not-real"))
    }
}
