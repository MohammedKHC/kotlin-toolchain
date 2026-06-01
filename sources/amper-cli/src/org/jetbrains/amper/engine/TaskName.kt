/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.engine

import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.table.HorizontalLayoutBuilder
import org.jetbrains.amper.frontend.AmperModule
import org.jetbrains.amper.frontend.Fragment
import org.jetbrains.amper.frontend.Platform
import org.jetbrains.amper.frontend.TaskId
import org.jetbrains.amper.frontend.doCapitalize
import org.jetbrains.amper.tasks.ProjectTasksBuilder.Companion.testSuffix
import org.jetbrains.amper.util.BuildType

/**
 * Represents a [Task] name;
 * Provides an internal [id] for the execution engine purposes;
 * and a user-visible [renderOperationMonikerWidget] as well.
 *
 * WARNING: This class doesn't provide data-like [Any.equals]/[Any.hashCode] implementation.
 *  Please use [id] for identity comparisons and in Sets/Maps.
 */
class TaskName(
    /**
     * Engine-level internal task id that identifies the task.
     */
    val id: TaskId,
    /**
     * A builder for creating a user-visible presentation of the root task operation in the status widget.
     *
     * TODO: Maybe use Bundle here somehow?
     */
    val renderOperationMonikerWidget: context(Theme) HorizontalLayoutBuilder.() -> Unit,
)

/**
 * Constructs a project-scoped (global) task name.
 *
 * @param internalName a part of the [TaskId]
 * @param operationMoniker user-readable operation moniker.
 */
fun TaskName(
    internalName: String,
    operationMoniker: String,
): TaskName {
    require(operationMoniker.isNotBlank()) { "blank `operationMoniker`" }
    return TaskName(
        id = TaskId(internalName),
        renderOperationMonikerWidget = {
            cell(Markdown(operationMoniker)) {
                style(bold = true)
            }
        },
    )
}

/**
 * Constructs a module-scoped task name.
 *
 * NOTE: Prefer using a [org.jetbrains.amper.tasks.TaskNameFactory.Module] for non-adhoc tasks.
 *
 * @param internalName a part of the [TaskId]
 * @param operationMoniker user-readable operation moniker.
 */
fun TaskName(
    module: AmperModule,
    internalName: String,
    operationMoniker: String,
): TaskName {
    require(operationMoniker.isNotBlank()) { "blank `operationMoniker`" }
    return TaskName(
        id = TaskId.moduleTask(module, internalName),
        renderOperationMonikerWidget = {
            renderModule(module)
            cell(Markdown(operationMoniker)) {
                style(bold = true)
            }
        },
    )
}

/**
 * Constructs a compilation-scoped task name. Given parameters describe a compilation.
 *
 * NOTE: Prefer using a [org.jetbrains.amper.tasks.TaskNameFactory.LeafPlatform] for non-adhoc tasks.
 *
 * @param internalName a part of the [TaskId]
 * @param operationMoniker user-readable operation moniker.
 */
fun TaskName(
    module: AmperModule,
    platform: Platform,
    isTest: Boolean = false,
    buildType: BuildType? = null,
    suffix: String = "",
    internalName: String,
    operationMoniker: String,
): TaskName {
    require(operationMoniker.isNotBlank()) { "blank `operationMoniker`" }
    require(platform.isLeaf) { "$platform is not a leaf platform" }
    require(platform != Platform.JVM || buildType == null) { "BuildType must not be present in task names for JVM" }

    val uppercasePlatform = platform.pretty.replaceFirstChar { it.uppercase() }
    val buildTypeSuffix = buildType?.name ?: ""
    val testSuffix = isTest.testSuffix
    return TaskName(
        id = TaskId.moduleTask(module, "${internalName}$uppercasePlatform$testSuffix$buildTypeSuffix$suffix"),
        renderOperationMonikerWidget = {
            renderModule(module)
            val theme = contextOf<Theme>()

            cell(Markdown(operationMoniker)) {
                style(bold = true)
            }

            cell(theme.muted("[${platform.pretty}]"))
            if (buildType != null) cell(theme.muted("[${buildType.value}]"))
            // NOTE: We ignore suffix here

            if (isTest) cell(theme.muted("for unit tests"))
        }
    )
}

/**
 * Constructs a fragment-scoped task name.
 *
 * NOTE: Prefer using a [org.jetbrains.amper.tasks.TaskNameFactory.Fragment] for non-adhoc tasks.
 *
 * @param internalName a part of the [TaskId]
 * @param operationMoniker user-readable operation moniker.
 */
fun TaskName(
    fragment: Fragment,
    internalName: String,
    operationMoniker: String,
): TaskName {
    require(operationMoniker.isNotBlank()) { "blank `operationMoniker`" }
    return TaskName(
        id = TaskId.moduleTask(fragment.module, "$internalName${fragment.name.doCapitalize()}"),
        renderOperationMonikerWidget = {
            renderModule(fragment.module)
            val theme = contextOf<Theme>()
            cell(Markdown(operationMoniker)) {
                style(bold = true)
            }
            cell(theme.muted("[${fragment.name}]"))
        }
    )
}

/**
 * Helper function to prefix the [TaskName.renderOperationMonikerWidget] with a [module].
 */
context(theme: Theme)
fun HorizontalLayoutBuilder.renderModule(module: AmperModule) {
    cell(theme.muted("module ") + theme.info(module.userReadableName) + theme.muted(":"))
}
