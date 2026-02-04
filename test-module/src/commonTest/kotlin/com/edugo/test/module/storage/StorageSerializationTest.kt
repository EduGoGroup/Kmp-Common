/*
 * Copyright (c) 2026 EduGo Project
 * Licensed under the MIT License
 */

package com.edugo.test.module.storage

import com.edugo.test.module.core.Result
import com.russhwolf.settings.MapSettings
import kotlinx.serialization.Serializable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Serializable
data class TestUser(val id: Int, val name: String, val email: String)

@Serializable
data class TestAddress(val street: String, val city: String, val zip: String)

@Serializable
data class TestUserWithAddress(
    val user: TestUser,
    val address: TestAddress
)

class StorageSerializationTest {

    private lateinit var storage: EduGoStorage

    @BeforeTest
    fun setup() {
        storage = EduGoStorage.withSettings(MapSettings())
    }

    // ===== OBJETO SIMPLE =====

    @Test
    fun putObject_and_getObject_works_for_simple_object() {
        val user = TestUser(1, "John", "john@test.com")

        storage.putObject("user", user)
        val retrieved = storage.getObject<TestUser>("user")

        assertEquals(user, retrieved)
    }

    @Test
    fun getObject_returns_null_for_missing_key() {
        val result = storage.getObject<TestUser>("missing")
        assertNull(result)
    }

    @Test
    fun getObject_with_default_returns_default_for_missing_key() {
        val default = TestUser(0, "Guest", "guest@test.com")
        val result = storage.getObject("missing", default)
        assertEquals(default, result)
    }

    @Test
    fun getObject_with_default_returns_stored_value_when_exists() {
        val stored = TestUser(1, "John", "john@test.com")
        val default = TestUser(0, "Guest", "guest@test.com")

        storage.putObject("user", stored)
        val result = storage.getObject("user", default)

        assertEquals(stored, result)
    }

    // ===== OBJETO ANIDADO =====

    @Test
    fun putObject_and_getObject_works_for_nested_objects() {
        val userWithAddress = TestUserWithAddress(
            user = TestUser(1, "Jane", "jane@test.com"),
            address = TestAddress("123 Main St", "NYC", "10001")
        )

        storage.putObject("userWithAddress", userWithAddress)
        val retrieved = storage.getObject<TestUserWithAddress>("userWithAddress")

        assertEquals(userWithAddress, retrieved)
    }

    @Test
    fun nested_object_preserves_all_fields() {
        val userWithAddress = TestUserWithAddress(
            user = TestUser(42, "Alice", "alice@example.com"),
            address = TestAddress("456 Oak Ave", "Los Angeles", "90001")
        )

        storage.putObject("data", userWithAddress)
        val retrieved = storage.getObject<TestUserWithAddress>("data")!!

        assertEquals(42, retrieved.user.id)
        assertEquals("Alice", retrieved.user.name)
        assertEquals("alice@example.com", retrieved.user.email)
        assertEquals("456 Oak Ave", retrieved.address.street)
        assertEquals("Los Angeles", retrieved.address.city)
        assertEquals("90001", retrieved.address.zip)
    }

    // ===== RESULT - SUCCESS =====

    @Test
    fun getObjectSafe_returns_Success_for_valid_data() {
        val user = TestUser(1, "Test", "test@test.com")
        storage.putObject("user", user)

        val result = storage.getObjectSafe<TestUser>("user")

        assertTrue(result is Result.Success)
        assertEquals(user, (result as Result.Success).data)
    }

    @Test
    fun putObjectSafe_returns_Success_for_valid_object() {
        val user = TestUser(1, "Test", "test@test.com")

        val result = storage.putObjectSafe("user", user)

        assertTrue(result is Result.Success)
        assertEquals(user, storage.getObject<TestUser>("user"))
    }

    // ===== RESULT - FAILURE =====

    @Test
    fun getObjectSafe_returns_Failure_for_missing_key() {
        val result = storage.getObjectSafe<TestUser>("missing")

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error.contains("not found"))
    }

    @Test
    fun getObjectSafe_returns_Failure_for_corrupted_data() {
        storage.putString("corrupted", "not valid json{{{")

        val result = storage.getObjectSafe<TestUser>("corrupted")

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error.contains("deserialize"))
    }

    @Test
    fun getObjectSafe_returns_Failure_for_wrong_type() {
        storage.putString("wrongType", """{"wrong":"structure"}""")

        val result = storage.getObjectSafe<TestUser>("wrongType")

        assertTrue(result is Result.Failure)
    }

    // ===== SOBRESCRITURA =====

    @Test
    fun putObject_overwrites_existing_value() {
        val user1 = TestUser(1, "First", "first@test.com")
        val user2 = TestUser(2, "Second", "second@test.com")

        storage.putObject("user", user1)
        storage.putObject("user", user2)

        val retrieved = storage.getObject<TestUser>("user")
        assertEquals(user2, retrieved)
    }

    // ===== DELEGATED PROPERTIES =====

    @Test
    fun serializable_delegate_reads_and_writes() {
        var user: TestUser? by storage.serializable("delegate.user")

        assertNull(user)

        user = TestUser(1, "Delegate", "delegate@test.com")
        assertEquals(TestUser(1, "Delegate", "delegate@test.com"), user)

        user = null
        assertNull(user)
    }

    @Test
    fun serializableWithDefault_delegate_returns_default_when_missing() {
        val default = TestUser(0, "Default", "default@test.com")
        var user: TestUser by storage.serializableWithDefault("delegate.user") { default }

        assertEquals(default, user)

        user = TestUser(1, "Updated", "updated@test.com")
        assertEquals(TestUser(1, "Updated", "updated@test.com"), user)
    }
}
