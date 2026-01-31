package com.edugo.test.module.platform

/**
 * Utilities for parsing and extracting information from hierarchical log tags.
 *
 * Provides helpers for working with dot-separated hierarchical tags,
 * extracting modules, features, and components.
 *
 * ## Tag structure conventions:
 * - `Module.Feature.Component` (e.g., "EduGo.Auth.Login")
 * - `Package.Class` (e.g., "com.edugo.UserRepository")
 * - `Service.Operation` (e.g., "API.UserCreate")
 *
 * @see TaggedLogger
 */
object TagParser {
    /**
     * Extracts the root module from a hierarchical tag.
     *
     * Returns the first segment of a dot-separated tag.
     *
     * @param tag The hierarchical tag
     * @return The root module, or the tag itself if no dots
     *
     * Example:
     * ```kotlin
     * TagParser.getModule("EduGo.Auth.Login")  // "EduGo"
     * TagParser.getModule("UserRepository")    // "UserRepository"
     * ```
     */
    fun getModule(tag: String): String {
        val dotIndex = tag.indexOf('.')
        return if (dotIndex > 0) {
            tag.substring(0, dotIndex)
        } else {
            tag
        }
    }

    /**
     * Extracts the feature/submodule from a hierarchical tag.
     *
     * Returns the second segment of a dot-separated tag, or null if
     * the tag has fewer than 2 segments.
     *
     * @param tag The hierarchical tag
     * @return The feature name, or null if not present
     *
     * Example:
     * ```kotlin
     * TagParser.getFeature("EduGo.Auth.Login")       // "Auth"
     * TagParser.getFeature("EduGo.Auth")             // "Auth"
     * TagParser.getFeature("EduGo")                  // null
     * ```
     */
    fun getFeature(tag: String): String? {
        val segments = tag.split('.')
        return if (segments.size >= 2) segments[1] else null
    }

    /**
     * Extracts the component/class from a hierarchical tag.
     *
     * Returns the last segment of a dot-separated tag.
     *
     * @param tag The hierarchical tag
     * @return The component name
     *
     * Example:
     * ```kotlin
     * TagParser.getComponent("EduGo.Auth.Login")     // "Login"
     * TagParser.getComponent("UserRepository")       // "UserRepository"
     * ```
     */
    fun getComponent(tag: String): String {
        val lastDotIndex = tag.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < tag.length - 1) {
            tag.substring(lastDotIndex + 1)
        } else {
            tag
        }
    }

    /**
     * Gets the parent tag (all segments except the last).
     *
     * @param tag The hierarchical tag
     * @return The parent tag, or null if no parent
     *
     * Example:
     * ```kotlin
     * TagParser.getParent("EduGo.Auth.Login")  // "EduGo.Auth"
     * TagParser.getParent("EduGo.Auth")        // "EduGo"
     * TagParser.getParent("EduGo")             // null
     * ```
     */
    fun getParent(tag: String): String? {
        val lastDotIndex = tag.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            tag.substring(0, lastDotIndex)
        } else {
            null
        }
    }

    /**
     * Splits a tag into all its hierarchical segments.
     *
     * @param tag The hierarchical tag
     * @return List of tag segments
     *
     * Example:
     * ```kotlin
     * TagParser.getSegments("EduGo.Auth.Login")
     * // ["EduGo", "Auth", "Login"]
     * ```
     */
    fun getSegments(tag: String): List<String> {
        return tag.split('.')
    }

    /**
     * Gets all ancestor tags (parent, grandparent, etc.).
     *
     * @param tag The hierarchical tag
     * @return List of ancestor tags from immediate parent to root
     *
     * Example:
     * ```kotlin
     * TagParser.getAncestors("EduGo.Auth.Login.OAuth")
     * // ["EduGo.Auth.Login", "EduGo.Auth", "EduGo"]
     * ```
     */
    fun getAncestors(tag: String): List<String> {
        val ancestors = mutableListOf<String>()
        var current = tag
        while (true) {
            val parent = getParent(current) ?: break
            ancestors.add(parent)
            current = parent
        }
        return ancestors
    }

    /**
     * Gets the depth (number of segments) of a tag.
     *
     * @param tag The hierarchical tag
     * @return Number of segments (minimum 1)
     *
     * Example:
     * ```kotlin
     * TagParser.getDepth("EduGo.Auth.Login")  // 3
     * TagParser.getDepth("EduGo")             // 1
     * ```
     */
    fun getDepth(tag: String): Int {
        return getSegments(tag).size
    }

    /**
     * Checks if a tag is a child of another tag.
     *
     * @param tag The tag to check
     * @param parent The potential parent tag
     * @return true if tag is a direct or indirect child of parent
     *
     * Example:
     * ```kotlin
     * TagParser.isChildOf("EduGo.Auth.Login", "EduGo.Auth")    // true
     * TagParser.isChildOf("EduGo.Auth.Login", "EduGo")         // true
     * TagParser.isChildOf("EduGo.Auth", "EduGo.Network")       // false
     * ```
     */
    fun isChildOf(tag: String, parent: String): Boolean {
        return tag.startsWith("$parent.")
    }

    /**
     * Checks if a tag is a direct child (immediate child, not grandchild).
     *
     * @param tag The tag to check
     * @param parent The potential parent tag
     * @return true if tag is a direct child of parent
     *
     * Example:
     * ```kotlin
     * TagParser.isDirectChildOf("EduGo.Auth.Login", "EduGo.Auth")  // true
     * TagParser.isDirectChildOf("EduGo.Auth.Login", "EduGo")       // false
     * ```
     */
    fun isDirectChildOf(tag: String, parent: String): Boolean {
        return getParent(tag) == parent
    }

    /**
     * Builds a child tag from a parent and child name.
     *
     * @param parent The parent tag
     * @param childName The child segment name
     * @return The combined child tag
     *
     * Example:
     * ```kotlin
     * TagParser.buildChildTag("EduGo.Auth", "Login")  // "EduGo.Auth.Login"
     * ```
     */
    fun buildChildTag(parent: String, childName: String): String {
        require(childName.isNotBlank()) { "Child name cannot be blank" }
        require(!childName.contains('.')) { "Child name cannot contain dots" }
        return "$parent.$childName"
    }

    /**
     * Normalizes a tag by removing extra dots and whitespace.
     *
     * @param tag The tag to normalize
     * @return Normalized tag
     *
     * Example:
     * ```kotlin
     * TagParser.normalize("  EduGo..Auth.Login  ")  // "EduGo.Auth.Login"
     * TagParser.normalize(".EduGo.Auth.")           // "EduGo.Auth"
     * ```
     */
    fun normalize(tag: String): String {
        return tag.trim()
            .split('.')
            .filter { it.isNotBlank() }
            .joinToString(".")
    }

    /**
     * Validates if a tag is well-formed.
     *
     * A well-formed tag:
     * - Is not blank
     * - Does not start or end with '.'
     * - Does not contain consecutive dots
     * - Contains only valid characters (alphanumeric, underscore, dot)
     *
     * @param tag The tag to validate
     * @return true if valid, false otherwise
     *
     * Example:
     * ```kotlin
     * TagParser.isValid("EduGo.Auth.Login")  // true
     * TagParser.isValid(".EduGo")            // false (starts with dot)
     * TagParser.isValid("EduGo..Auth")       // false (consecutive dots)
     * ```
     */
    fun isValid(tag: String): Boolean {
        if (tag.isBlank()) return false
        if (tag.startsWith('.') || tag.endsWith('.')) return false
        if (tag.contains("..")) return false
        // Check for valid characters (alphanumeric, underscore, dot, hyphen)
        return tag.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
    }

    /**
     * Gets the relative path from one tag to another.
     *
     * @param fromTag The starting tag
     * @param toTag The target tag
     * @return The relative tag path, or null if not related
     *
     * Example:
     * ```kotlin
     * TagParser.getRelativePath("EduGo.Auth", "EduGo.Auth.Login.OAuth")
     * // "Login.OAuth"
     *
     * TagParser.getRelativePath("EduGo.Auth", "EduGo.Network")
     * // null (not related)
     * ```
     */
    fun getRelativePath(fromTag: String, toTag: String): String? {
        return if (toTag.startsWith("$fromTag.")) {
            toTag.substring(fromTag.length + 1)
        } else {
            null
        }
    }
}
