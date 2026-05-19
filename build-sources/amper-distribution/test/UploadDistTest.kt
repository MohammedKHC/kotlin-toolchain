/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class UploadDistTest {
    @Test
    fun `upload artifacts include sdkman archive under sdkman artifact id`() {
        val tempDir = createTempDirectory()
        try {
            val distribution = object : Distribution {
                override val cliTgz: Path = tempDir / "cli.tgz"
                override val wrappersDir: Path = tempDir / "wrappers"
                override val installersDir: Path = (tempDir / "installers").also { it.createDirectories() }
            }
            distribution.cliTgz.writeText("fake distribution")
            distribution.wrappersDir.createDirectories()
            (distribution.wrappersDir / "kotlin").writeText("unix wrapper")

            val sdkmanArchive = tempDir / "kotlin.zip"
            sdkmanArchive.writeText("sdkman archive")

            val artifactTempDir = tempDir / "artifacts"
            artifactTempDir.createDirectories()
            val artifacts = context(artifactTempDir) {
                distArtifacts(distribution, sdkmanArchive)
            }

            val sdkmanZip = artifacts.single { it.artifactId == "kotlin-cli-sdkman" && it.extension == "zip" }
            assertEquals("", sdkmanZip.classifier)
            assertEquals(sdkmanArchive.toFile(), sdkmanZip.file)

            val sdkmanPom = artifacts.single { it.artifactId == "kotlin-cli-sdkman" && it.extension == "pom" }
            assertContains(sdkmanPom.file.toPath().readText(), "<artifactId>kotlin-cli-sdkman</artifactId>")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
