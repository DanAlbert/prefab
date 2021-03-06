/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.prefab.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.google.prefab.api.Package
import com.google.prefab.api.PlatformDataInterface
import com.google.prefab.api.PlatformRegistry
import java.io.File

/**
 * A fatal error which will cause the application to exit.
 */
open class FatalApplicationError(message: String) : RuntimeException(message)

/**
 * Package names are not unique across the given configuration.
 */
class DuplicatePackageNamesException(a: Package, b: Package) :
    FatalApplicationError(
        "Multiple packages named ${a.name} found: ${a.path} and ${b.path}."
    )

// Open for testing.
/**
 * The command line interface for Prefab.
 */
open class Cli : CliktCommand(help = "prefab") {
    private val buildSystem: String by option(
        help = "Generate integration for the given build system."
    ).required()

    private val output: File by option(
        help = "Output path for generated build system integration."
    ).file(fileOkay = false).required()

    private val pluginPath: List<File> by option(
        help = "Path to build system integration plugin."
    ).file(folderOkay = false, readable = true).multiple()

    // TODO: --help should list supported platforms.
    private val platform: String by option(
        help = "Target platform."
    ).required().validate {
        require(
            PlatformRegistry.supports(it)
        ) { "unsupported platform requested" }
    }

    private val abi: String? by option(help = "Target ABI.")
    private val osVersion: String? by option(help = "Target OS version.")

    private val rawPackagePaths: List<File> by argument().file(
        fileOkay = false, readable = true
    ).multiple().validate {
        require(it.isNotEmpty()) { "must provide at least one package" }
    }

    /**
     * De-duplicated packages paths.
     */
    private val packagePaths: Set<File> by lazy { rawPackagePaths.toSet() }

    /**
     * All packages to be processed.
     */
    private val packages: List<Package> by lazy {
        packagePaths.map { Package(it.toPath()) }
    }

    private val platformRequirements: Collection<PlatformDataInterface>
            by lazy {
                PlatformRegistry.find(platform)!!.fromCommandLineArgs(
                    abi,
                    osVersion
                )
            }

    /**
     * Verifies that the configuration requested by the user is valid.
     *
     * Most options are checked as part of their own validation steps. This
     * function validates that all options are mutually compatible and anything
     * that must be validated late (such as checking for valid build systems,
     * since the list of supported build systems is not known until plugins are
     * loaded).
     */
    protected fun validate() {
        platformRequirements // Force lazy initialization.
        if (!BuildSystemRegistry.supports(buildSystem)) {
            throw UsageError("unsupported build system requested")
        }

        val seenPackageNames = mutableMapOf<String, Package>()
        for (pkg in packages) {
            val seenPackage = seenPackageNames[pkg.name]
            if (seenPackage != null) {
                throw DuplicatePackageNamesException(pkg, seenPackage)
            }

            seenPackageNames[pkg.name] = pkg
        }
    }

    /**
     * CLI entry point.
     *
     * Validates user input, parses and validates packages provided by the user,
     * and generates the integration for the requested build system.
     */
    override fun run() {
        validate()

        val knownPackages = packages.map { it.name }.toSet()
        for (pkg in packages) {
            for (dep in pkg.dependencies) {
                if (!knownPackages.contains(dep)) {
                    throw FatalApplicationError(
                        "Error: ${pkg.name} depends on unknown dependency $dep"
                    )
                }
            }
        }

        val buildSystemIntegration =
            BuildSystemRegistry.find(buildSystem)!!.create(output, packages)

        buildSystemIntegration.generate(platformRequirements)
    }
}
