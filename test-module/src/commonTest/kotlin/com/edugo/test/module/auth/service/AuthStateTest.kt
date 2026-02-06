package com.edugo.test.module.auth.service

import com.edugo.test.module.auth.service.AuthState
import com.edugo.test.module.auth.model.AuthUserInfo
import com.edugo.test.module.data.models.AuthToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests para [AuthState] y sus helper functions.
 *
 * Verifica:
 * - Estados básicos (Unauthenticated, Authenticated, Loading)
 * - Helper properties (isAuthenticated, isUnauthenticated, isLoading)
 * - Extension functions (ifAuthenticated, ifUnauthenticated, fold)
 * - Getters de usuario y token
 */
class AuthStateTest {

    private val testUser = AuthUserInfo.createTestUser()
    private val testToken = AuthToken.createTestToken()

    @Test
    fun testUnauthenticatedState() {
        val state = AuthState.Unauthenticated

        assertFalse(state.isAuthenticated)
        assertTrue(state.isUnauthenticated)
        assertFalse(state.isLoading)
        assertNull(state.currentUser)
        assertNull(state.currentToken)
    }

    @Test
    fun testAuthenticatedState() {
        val state = AuthState.Authenticated(testUser, testToken)

        assertTrue(state.isAuthenticated)
        assertFalse(state.isUnauthenticated)
        assertFalse(state.isLoading)
        assertEquals(testUser, state.currentUser)
        assertEquals(testToken, state.currentToken)
    }

    @Test
    fun testLoadingState() {
        val state = AuthState.Loading

        assertFalse(state.isAuthenticated)
        assertFalse(state.isUnauthenticated)
        assertTrue(state.isLoading)
        assertNull(state.currentUser)
        assertNull(state.currentToken)
    }

    @Test
    fun testIfAuthenticatedExtensionExecutes() {
        val authenticatedState = AuthState.Authenticated(testUser, testToken)
        var called = false

        authenticatedState.ifAuthenticated { user, token ->
            called = true
            assertEquals(testUser, user)
            assertEquals(testToken, token)
        }

        assertTrue(called, "ifAuthenticated should execute for Authenticated state")
    }

    @Test
    fun testIfAuthenticatedExtensionDoesNotExecuteForUnauthenticated() {
        val unauthenticatedState = AuthState.Unauthenticated
        var called = false

        unauthenticatedState.ifAuthenticated { _, _ ->
            called = true
        }

        assertFalse(called, "ifAuthenticated should not execute for Unauthenticated state")
    }

    @Test
    fun testIfUnauthenticatedExtensionExecutes() {
        val unauthenticatedState = AuthState.Unauthenticated
        var called = false

        unauthenticatedState.ifUnauthenticated {
            called = true
        }

        assertTrue(called, "ifUnauthenticated should execute for Unauthenticated state")
    }

    @Test
    fun testIfUnauthenticatedExtensionDoesNotExecuteForAuthenticated() {
        val authenticatedState = AuthState.Authenticated(testUser, testToken)
        var called = false

        authenticatedState.ifUnauthenticated {
            called = true
        }

        assertFalse(called, "ifUnauthenticated should not execute for Authenticated state")
    }

    @Test
    fun testFoldPatternMatchingForAuthenticated() {
        val authenticatedState = AuthState.Authenticated(testUser, testToken)

        val result = authenticatedState.fold(
            onAuthenticated = { user, _ -> user.email },
            onUnauthenticated = { "not authenticated" },
            onLoading = { "loading" }
        )

        assertEquals(testUser.email, result)
    }

    @Test
    fun testFoldPatternMatchingForUnauthenticated() {
        val unauthenticatedState = AuthState.Unauthenticated

        val result = unauthenticatedState.fold(
            onAuthenticated = { _, _ -> "authenticated" },
            onUnauthenticated = { "not authenticated" },
            onLoading = { "loading" }
        )

        assertEquals("not authenticated", result)
    }

    @Test
    fun testFoldPatternMatchingForLoading() {
        val loadingState = AuthState.Loading

        val result = loadingState.fold(
            onAuthenticated = { _, _ -> "authenticated" },
            onUnauthenticated = { "not authenticated" },
            onLoading = { "loading" }
        )

        assertEquals("loading", result)
    }

    @Test
    fun testAuthenticatedStateCopy() {
        val original = AuthState.Authenticated(testUser, testToken)
        val newToken = AuthToken.createTestToken(durationSeconds = 7200)

        val copied = original.copy(token = newToken)

        assertEquals(testUser, copied.user)
        assertEquals(newToken, copied.token)
        assertTrue(copied.isAuthenticated)
    }

    @Test
    fun testCurrentUserIsNullForLoading() {
        val state = AuthState.Loading
        assertNull(state.currentUser)
    }

    @Test
    fun testCurrentTokenIsNullForLoading() {
        val state = AuthState.Loading
        assertNull(state.currentToken)
    }
}
