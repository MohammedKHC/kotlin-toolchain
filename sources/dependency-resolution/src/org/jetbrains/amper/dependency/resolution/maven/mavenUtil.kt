/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.dependency.resolution.maven

import org.jetbrains.amper.dependency.resolution.MavenRepository
import org.jetbrains.amper.dependency.resolution.MavenRepository.Companion.MavenCentral

/**
 * This method is intended to be used by Amper libraries only.
 * It is not a part of a public API and is subject to change.
 *
 * Normally, [MavenCentral] should be used whenever maven central repository is required by client code.
 */
fun mavenCentralOrProxy(): MavenRepository =
    if (System.getenv("AMPER_OVERRIDE_MAVEN_CENTRAL_URL_TO_CACHE_REDIRECTOR")?.toBooleanStrictOrNull() == true) {
        MavenRepository("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
    } else {
        MavenCentral
    }