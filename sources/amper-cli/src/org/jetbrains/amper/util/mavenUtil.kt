/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.util

import org.jetbrains.amper.dependency.resolution.MavenRepository
import org.jetbrains.amper.dependency.resolution.MavenRepository.Companion.MavenCentral
import org.jetbrains.amper.mavencentral.MavenCentralDefaultConfiguration

internal fun mavenCentralOrProxy(): MavenRepository =
    if (MavenCentralDefaultConfiguration.isDirectUrl) {
        MavenCentral
    } else {
        MavenRepository(MavenCentralDefaultConfiguration.url)
    }