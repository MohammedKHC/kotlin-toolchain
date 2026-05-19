/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.mavencentral

/**
 * Provides URL of Maven Central.
 * By default, cache-redirector is used for accessing Maven Central.
 * Default value can be overridden by env variable "KOTLIN_DEFAULT_MAVEN_CENTRAL_URL".
 *
 * The resulting default value might be further overridden in a particular project module by specifying
 *  a repository with ID "mavenCentral" and setting another URL there.
 */
object MavenCentralDefaultConfiguration {

    val url: String
        get() = System.getenv("KOTLIN_DEFAULT_MAVEN_CENTRAL_URL")
            ?.takeIf { it.isNotBlank() }
            ?: "https://repo1.maven.org/maven2"

    val isDirectUrl: Boolean get() = url.trimEnd('/') in directUrls

    private val directUrls = setOf(
        "https://repo1.maven.org/maven2",
        "https://repo.maven.apache.org/maven2"
    )
}

