package com.edugo.test.module.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagParserTest {

    @Test
    fun testGetModule() {
        assertEquals("EduGo", TagParser.getModule("EduGo.Auth.Login"))
        assertEquals("EduGo", TagParser.getModule("EduGo"))
        assertEquals("com", TagParser.getModule("com.edugo.UserRepository"))
    }

    @Test
    fun testGetFeature() {
        assertEquals("Auth", TagParser.getFeature("EduGo.Auth.Login"))
        assertEquals("Auth", TagParser.getFeature("EduGo.Auth"))
        assertNull(TagParser.getFeature("EduGo"))
    }

    @Test
    fun testGetComponent() {
        assertEquals("Login", TagParser.getComponent("EduGo.Auth.Login"))
        assertEquals("UserRepository", TagParser.getComponent("UserRepository"))
        assertEquals("OAuth", TagParser.getComponent("EduGo.Auth.Login.OAuth"))
    }

    @Test
    fun testGetParent() {
        assertEquals("EduGo.Auth", TagParser.getParent("EduGo.Auth.Login"))
        assertEquals("EduGo", TagParser.getParent("EduGo.Auth"))
        assertNull(TagParser.getParent("EduGo"))
    }

    @Test
    fun testGetSegments() {
        assertEquals(listOf("EduGo", "Auth", "Login"), TagParser.getSegments("EduGo.Auth.Login"))
        assertEquals(listOf("EduGo"), TagParser.getSegments("EduGo"))
    }

    @Test
    fun testGetAncestors() {
        val ancestors = TagParser.getAncestors("EduGo.Auth.Login.OAuth")
        assertEquals(3, ancestors.size)
        assertEquals("EduGo.Auth.Login", ancestors[0])
        assertEquals("EduGo.Auth", ancestors[1])
        assertEquals("EduGo", ancestors[2])

        assertTrue(TagParser.getAncestors("EduGo").isEmpty())
    }

    @Test
    fun testGetDepth() {
        assertEquals(3, TagParser.getDepth("EduGo.Auth.Login"))
        assertEquals(1, TagParser.getDepth("EduGo"))
        assertEquals(4, TagParser.getDepth("EduGo.Auth.Login.OAuth"))
    }

    @Test
    fun testIsChildOf() {
        assertTrue(TagParser.isChildOf("EduGo.Auth.Login", "EduGo.Auth"))
        assertTrue(TagParser.isChildOf("EduGo.Auth.Login", "EduGo"))
        assertTrue(TagParser.isChildOf("EduGo.Auth.Login.OAuth", "EduGo.Auth"))
        assertFalse(TagParser.isChildOf("EduGo.Auth", "EduGo.Network"))
        assertFalse(TagParser.isChildOf("EduGo", "EduGo.Auth"))
    }

    @Test
    fun testIsDirectChildOf() {
        assertTrue(TagParser.isDirectChildOf("EduGo.Auth.Login", "EduGo.Auth"))
        assertFalse(TagParser.isDirectChildOf("EduGo.Auth.Login", "EduGo"))
        assertFalse(TagParser.isDirectChildOf("EduGo.Auth.Login.OAuth", "EduGo.Auth"))
    }

    @Test
    fun testBuildChildTag() {
        assertEquals("EduGo.Auth.Login", TagParser.buildChildTag("EduGo.Auth", "Login"))
    }

    @Test
    fun testNormalize() {
        assertEquals("EduGo.Auth.Login", TagParser.normalize("  EduGo..Auth.Login  "))
        assertEquals("EduGo.Auth", TagParser.normalize(".EduGo.Auth."))
        assertEquals("EduGo", TagParser.normalize("EduGo"))
    }

    @Test
    fun testIsValid() {
        assertTrue(TagParser.isValid("EduGo.Auth.Login"))
        assertTrue(TagParser.isValid("com.edugo.UserRepository"))
        assertTrue(TagParser.isValid("EduGo_Auth"))
        assertTrue(TagParser.isValid("EduGo-Auth"))

        assertFalse(TagParser.isValid(""))
        assertFalse(TagParser.isValid("  "))
        assertFalse(TagParser.isValid(".EduGo"))
        assertFalse(TagParser.isValid("EduGo."))
        assertFalse(TagParser.isValid("EduGo..Auth"))
    }

    @Test
    fun testGetRelativePath() {
        assertEquals("Login.OAuth", TagParser.getRelativePath("EduGo.Auth", "EduGo.Auth.Login.OAuth"))
        assertEquals("Login", TagParser.getRelativePath("EduGo.Auth", "EduGo.Auth.Login"))
        assertNull(TagParser.getRelativePath("EduGo.Auth", "EduGo.Network"))
        assertNull(TagParser.getRelativePath("EduGo.Auth", "EduGo"))
    }
}
