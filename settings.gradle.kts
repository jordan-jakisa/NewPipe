/*
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "YouFlow"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://repo.clojars.org")
    }
}
include(":app") // androidApp

// NewPipe Extractor is a git submodule pinned to the commit in gradle/libs.versions.toml. It is built
// as part of this build (JitPack never built that commit), so a fresh clone needs
// `git submodule update --init`.
includeBuild("NewPipeExtractor") {
    dependencySubstitution {
        substitute(module("com.github.TeamNewPipe:NewPipeExtractor"))
            .using(project(":extractor"))
    }
}
