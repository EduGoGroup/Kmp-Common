package com.edugo.test.module.auth.jwt

import kotlin.test.*

class JwtParserTest {

    // Token de prueba válido (generado para tests)
    // Header: {"alg":"HS256","typ":"JWT"}
    // Payload: {"sub":"user-123","iss":"edugo-api","exp":1893456000,"iat":1704067200,"role":"admin","school_id":"school-456"}
    private val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTEyMyIsImlzcyI6ImVkdWdvLWFwaSIsImV4cCI6MTg5MzQ1NjAwMCwiaWF0IjoxNzA0MDY3MjAwLCJyb2xlIjoiYWRtaW4iLCJzY2hvb2xfaWQiOiJzY2hvb2wtNDU2In0.signature"

    // Token expirado (exp en el pasado)
    private val expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTEyMyIsImV4cCI6MTYwNDAwMDAwMH0.signature"

    @Test
    fun `parse valid token extracts claims correctly`() {
        val result = JwtParser.parse(validToken)

        assertTrue(result is JwtParseResult.Success)
        val claims = (result as JwtParseResult.Success).claims

        assertEquals("user-123", claims.subject)
        assertEquals("user-123", claims.userId)
        assertEquals("edugo-api", claims.issuer)
        assertEquals("admin", claims.role)
        assertEquals("school-456", claims.schoolId)
        assertNotNull(claims.expiresAt)
        assertNotNull(claims.issuedAt)
    }

    @Test
    fun `parse expired token returns Success with isExpired true`() {
        val result = JwtParser.parse(expiredToken)

        assertTrue(result is JwtParseResult.Success)
        val claims = (result as JwtParseResult.Success).claims
        assertTrue(claims.isExpired)
    }

    @Test
    fun `parse valid token with future exp returns isExpired false`() {
        val result = JwtParser.parse(validToken)

        val claims = (result as JwtParseResult.Success).claims
        assertFalse(claims.isExpired)
    }

    @Test
    fun `parse malformed token returns InvalidFormat`() {
        val result = JwtParser.parse("not.a.valid.jwt.token")

        assertTrue(result is JwtParseResult.InvalidFormat)
    }

    @Test
    fun `parse token with only 2 parts returns InvalidFormat`() {
        val result = JwtParser.parse("header.payload")

        assertTrue(result is JwtParseResult.InvalidFormat)
        assertTrue((result as JwtParseResult.InvalidFormat).reason.contains("3 parts"))
    }

    @Test
    fun `parse empty token returns EmptyToken`() {
        assertEquals(JwtParseResult.EmptyToken, JwtParser.parse(""))
        assertEquals(JwtParseResult.EmptyToken, JwtParser.parse("   "))
    }

    @Test
    fun `parse token with invalid base64 returns InvalidFormat`() {
        val result = JwtParser.parse("header.!!!invalid-base64!!!.signature")

        assertTrue(result is JwtParseResult.InvalidFormat)
    }

    @Test
    fun `isValidStructure returns true for valid structure`() {
        assertTrue(JwtParser.isValidStructure(validToken))
        assertTrue(JwtParser.isValidStructure("a.b.c"))
    }

    @Test
    fun `isValidStructure returns false for invalid structure`() {
        assertFalse(JwtParser.isValidStructure(""))
        assertFalse(JwtParser.isValidStructure("no-dots"))
        assertFalse(JwtParser.isValidStructure("one.dot"))
    }

    @Test
    fun `parseOrThrow returns claims for valid token`() {
        val claims = JwtParser.parseOrThrow(validToken)
        assertEquals("user-123", claims.userId)
    }

    @Test
    fun `parseOrThrow throws for invalid token`() {
        assertFailsWith<IllegalArgumentException> {
            JwtParser.parseOrThrow("invalid")
        }
    }

    @Test
    fun `custom claims are extracted correctly`() {
        val result = JwtParser.parse(validToken)
        val claims = (result as JwtParseResult.Success).claims

        assertEquals("admin", claims.customClaims["role"])
        assertEquals("school-456", claims.customClaims["school_id"])
    }
}
