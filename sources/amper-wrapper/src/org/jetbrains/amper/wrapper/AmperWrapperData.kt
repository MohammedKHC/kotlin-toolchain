/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.wrapper

import org.jetbrains.amper.system.info.OsFamily
import org.jetbrains.amper.system.info.SystemInfo
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

data class AmperWrapperData private constructor(
    val version: String,
    val sha256: String,
) {
    companion object {
        /**
         * Parses [AmperWrapperData] from the project with the directory [projectRoot].
         * The concrete wrapper script is selected based on the host platform.
         * If there is no wrapper there, returns `null`.
         */
        fun parseFromProjectRoot(
            projectRoot: Path,
        ): AmperWrapperData? {
            val platform = currentPlatform()
            val wrapperPath = projectRoot / platform.wrapperFileName
            if (!wrapperPath.isRegularFile()) {
                return null
            }
            return parse(wrapperPath = wrapperPath)
        }

        /**
         * Parses [AmperWrapperData] from the wrapper file at [wrapperPath].
         */
        fun parse(
            wrapperPath: Path,
        ): AmperWrapperData {
            val platform = if (wrapperPath.name == WrapperPlatform.Windows.wrapperFileName)
                WrapperPlatform.Windows else WrapperPlatform.Posix
            val amperWrapperText = wrapperPath.readText()

            // If any error arises from here, that means the wrapper format has changed and things need to be adjusted.
            return AmperWrapperData(
                version = checkNotNull(platform.versionRegex.find(amperWrapperText)) {
                    "Missing amper_version in the $wrapperPath"
                }.groupValues[1],
                sha256 = checkNotNull(platform.checkSumRegex.find(amperWrapperText)){
                    "Missing amper_sha256 in the $wrapperPath"
                }.groupValues[1],
            )
        }

        private fun currentPlatform() = when (SystemInfo.CurrentHost.family) {
            OsFamily.Windows -> WrapperPlatform.Windows
            else -> WrapperPlatform.Posix
        }

        private enum class WrapperPlatform(
            val versionRegex: Regex,
            val checkSumRegex: Regex,
            val wrapperFileName: String,
        ) {
            Windows(
                versionRegex = "^set amper_version=(.*)$".toRegex(RegexOption.MULTILINE),
                checkSumRegex = "^set amper_sha256=(.*)$".toRegex(RegexOption.MULTILINE),
                wrapperFileName = "amper.bat"
            ),
            Posix(
                versionRegex = "^amper_version=(.*)$".toRegex(RegexOption.MULTILINE),
                checkSumRegex = "^amper_sha256=(.*)$".toRegex(RegexOption.MULTILINE),
                wrapperFileName = "amper"
            ),
            ;
        }
    }
}
