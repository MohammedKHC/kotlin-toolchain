/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.cli.test

import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import org.jetbrains.amper.processes.runProcessAndCaptureOutput
import org.jetbrains.amper.system.info.OsFamily
import org.jetbrains.amper.system.info.SystemInfo
import org.jetbrains.amper.test.AmperCliWithWrapperTestBase
import org.jetbrains.amper.test.LocalAmperPublication
import org.jetbrains.amper.test.MacOrLinuxOnly
import org.jetbrains.amper.test.TempDirExtension
import org.jetbrains.amper.test.WindowsOnly
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.div
import kotlin.io.path.getPosixFilePermissions
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.setPosixFilePermissions
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmperInstallerTest : AmperCliWithWrapperTestBase() {
    @RegisterExtension
    private val tempDirExtension = TempDirExtension()

    @Test
    @MacOrLinuxOnly
    fun `installer works (bash)`() = runBlocking {
        val testHome = tempDirExtension.path / "home"
        val installer = LocalAmperPublication.installerSh
            .copyTo(tempDirExtension.path / "installer.sh")
            .let { it.setPosixFilePermissions(it.getPosixFilePermissions() + PosixFilePermission.OWNER_EXECUTE) }
        val result = runProcessAndCaptureOutput(
            command = listOf(installer.absolutePathString()),
            environment = mapOf(
                "HOME" to testHome.absolutePathString(),
                "SHELL" to "/bin/bash",
            ) + baseEnvironmentForWrapper(),
            redirectErrorStream = true,
        )

        println(result.stdout)

        assertEquals("", result.stderr.trim(), "Expected nothing in stderr")
        assertEquals(expected = 0, actual = result.exitCode, "Expected zero exit code")

        assertTrue(message = "Expected kotlin in .local/bin") {
            testHome.resolve(".local/bin/kotlin").isRegularFile()
        }
        val expectedProfileFile = testHome / when (SystemInfo.CurrentHost.family) {
            OsFamily.MacOs -> ".bash_profile"
            else -> ".bashrc"
        }
        assertEquals(
            expected = $$"""
                export PATH="$HOME/.local/bin:$PATH"
        """.trimIndent(),
            actual = expectedProfileFile.readText().trim()
        )

        assertContains(charSequence = result.stdout, other = "Kotlin Toolchain version 1.0-SNAPSHOT")
    }

    @Test
    @WindowsOnly
    fun `installer works (old powershell)`() = runBlocking {
        `installer works`("powershell.exe")
    }

    @Test
    @WindowsOnly
    fun `installer works (new powershell)`() = runBlocking {
        Assumptions.assumeTrue(hasNewPowershell(), "pwsh is not available")
        `installer works`("pwsh.exe")
    }

    private suspend fun `installer works`(
        powershell: String,
    ) {
        val testHome = tempDirExtension.path / "home"
        val result = runProcessAndCaptureOutput(
            command = listOf(powershell, "-File", LocalAmperPublication.installerPs1.absolutePathString()),
            environment = mapOf(
                "KOTLIN_CLI_NO_MODIFY_PATH" to "1",
                "USERPROFILE" to testHome.absolutePathString(),
            ) + baseEnvironmentForWrapper(),
            redirectErrorStream = true,
        )

        println(result.stdout)

        assertEquals("", result.stderr.trim(), "Expected nothing in stderr")
        assertEquals(expected = 0, actual = result.exitCode, "Expected zero exit code")

        assertTrue(message = "Expected kotlin.bat in .local/bin") {
            testHome.resolve(".local/bin/kotlin.bat").isRegularFile()
        }

        assertContains(charSequence = result.stdout, other = "Kotlin Toolchain version 1.0-SNAPSHOT")
    }

    private suspend fun hasNewPowershell(): Boolean = try {
        runProcessAndCaptureOutput(command = listOf("pwsh", "--version")).exitCode == 0
    } catch (_: IOException) {
        false
    }

    // TODO: Tests for more cases
}