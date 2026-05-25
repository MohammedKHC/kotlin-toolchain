/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.amper.core.extract.cleanDirectory
import org.jetbrains.amper.plugins.Classpath
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import org.jetbrains.amper.wrapper.AmperWrappers
import java.nio.file.Path
import kotlin.io.path.copyToRecursively
import kotlin.io.path.div
import kotlin.io.path.writeText

@TaskAction
fun buildUnpackedDist(
    @Output outputDir: Path,
    @Input baseClasspath: Classpath,
    @Input extraClasspaths: Map<String, Classpath> = emptyMap(),
    @Input extraFilteredClasspaths: Map<String, FilteredClasspath> = emptyMap(),
    @Input thirdPartyStagingDir: Path,
) {
    cleanDirectory(outputDir)

    copyWithDeduplication(outputDir / "lib", baseClasspath.resolvedFiles)
    extraClasspaths.forEach { (key, classpath) ->
        copyWithDeduplication(outputDir / key, classpath.resolvedFiles)
    }
    extraFilteredClasspaths.forEach { (key, classpath) ->
        copyWithDeduplication(outputDir / key, classpath.resolvedFiles)
    }

    AmperWrappers.generateLaunchers(outputDir / "bin")

    thirdPartyStagingDir.copyToRecursively(
        target = outputDir,
        followLinks = false,
        overwrite = false,
    )
}
