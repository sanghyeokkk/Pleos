package com.example.myapplication

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepairSearchTest {
    @Test fun parsesNamedPointsAndCentresAndSkipsIncompleteEntries() {
        val json = """{"elements":[{"lat":37.5,"lon":127.0,"tags":{"name":"정비소 A"}},{"center":{"lat":37.6,"lon":127.1},"tags":{"name":"정비소 B"}},{"lat":37.5,"lon":127.0,"tags":{}},{"lat":37.5,"lon":127.0,"tags":{"name":"정비소 A"}}]}"""
        val shops = parseRepairShops(json)
        assertEquals(2, shops.size)
        assertEquals("정비소 A", shops[0].name)
        assertEquals(37.6, shops[1].latitude, 0.0001)
    }
}
