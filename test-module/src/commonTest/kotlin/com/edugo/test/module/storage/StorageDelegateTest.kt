/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StorageDelegateTest {

    private lateinit var storage: EduGoStorage

    @BeforeTest
    fun setup() {
        storage = EduGoStorage.withSettings(MapSettings())
    }

    // ===== STRING DELEGATE =====
    @Test
    fun string_delegate_reads_and_writes() {
        var name by storage.string("user.name", "Guest")

        assertEquals("Guest", name) // Default
        name = "John"
        assertEquals("John", name)
        assertEquals("John", storage.getString("user.name"))
    }

    @Test
    fun stringOrNull_delegate_handles_null() {
        var token by storage.stringOrNull("auth.token")

        assertNull(token)
        token = "abc123"
        assertEquals("abc123", token)
        token = null
        assertNull(token)
        assertFalse(storage.contains("auth.token"))
    }

    // ===== INT DELEGATE =====
    @Test
    fun int_delegate_reads_and_writes() {
        var count by storage.int("counter", 0)

        assertEquals(0, count)
        count = 42
        assertEquals(42, count)
        assertEquals(42, storage.getInt("counter"))
    }

    @Test
    fun intOrNull_delegate_handles_null() {
        var score by storage.intOrNull("score")

        assertNull(score)
        score = 100
        assertEquals(100, score)
        score = null
        assertNull(score)
        assertFalse(storage.contains("score"))
    }

    // ===== LONG DELEGATE =====
    @Test
    fun long_delegate_reads_and_writes() {
        var timestamp by storage.long("timestamp", 0L)

        assertEquals(0L, timestamp)
        timestamp = 1234567890123L
        assertEquals(1234567890123L, timestamp)
    }

    @Test
    fun longOrNull_delegate_handles_null() {
        var expiry by storage.longOrNull("expiry")

        assertNull(expiry)
        expiry = 9999999999L
        assertEquals(9999999999L, expiry)
        expiry = null
        assertNull(expiry)
    }

    // ===== BOOLEAN DELEGATE =====
    @Test
    fun boolean_delegate_reads_and_writes() {
        var enabled by storage.boolean("feature.enabled", false)

        assertFalse(enabled)
        enabled = true
        assertTrue(enabled)
        assertTrue(storage.getBoolean("feature.enabled"))
    }

    @Test
    fun booleanOrNull_delegate_handles_null() {
        var accepted by storage.booleanOrNull("terms.accepted")

        assertNull(accepted)
        accepted = true
        assertEquals(true, accepted)
        accepted = null
        assertNull(accepted)
    }

    // ===== FLOAT DELEGATE =====
    @Test
    fun float_delegate_reads_and_writes() {
        var rate by storage.float("rate", 0f)

        assertEquals(0f, rate, 0.001f)
        rate = 3.14f
        assertEquals(3.14f, rate, 0.001f)
    }

    @Test
    fun floatOrNull_delegate_handles_null() {
        var percentage by storage.floatOrNull("percentage")

        assertNull(percentage)
        percentage = 0.75f
        assertEquals(0.75f, percentage!!, 0.001f)
        percentage = null
        assertNull(percentage)
    }

    // ===== DOUBLE DELEGATE =====
    @Test
    fun double_delegate_reads_and_writes() {
        var precise by storage.double("precise", 0.0)

        assertEquals(0.0, precise, 0.0000001)
        precise = 3.141592653589793
        assertEquals(3.141592653589793, precise, 0.0000001)
    }

    @Test
    fun doubleOrNull_delegate_handles_null() {
        var latitude by storage.doubleOrNull("latitude")

        assertNull(latitude)
        latitude = 40.7128
        assertEquals(40.7128, latitude!!, 0.0001)
        latitude = null
        assertNull(latitude)
    }

    // ===== MULTIPLE DELEGATES =====
    @Test
    fun multiple_delegates_on_same_storage() {
        var name by storage.string("name", "")
        var age by storage.int("age", 0)
        var active by storage.boolean("active", false)

        name = "Alice"
        age = 30
        active = true

        assertEquals("Alice", name)
        assertEquals(30, age)
        assertTrue(active)
    }

    @Test
    fun delegates_persist_across_reads() {
        var counter by storage.int("counter", 0)

        counter = 10
        counter = counter + 5
        counter = counter * 2

        assertEquals(30, counter)
        assertEquals(30, storage.getInt("counter"))
    }
}
