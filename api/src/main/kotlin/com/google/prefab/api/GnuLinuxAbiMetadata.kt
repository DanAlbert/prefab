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

package com.google.prefab.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The GNU/Linux abi.json schema.
 *
 * @property[arch] The architecture name of the described library. For a list of
 * valid architecture names, see [GnuLinux.Arch].
 * @property[glibcVersion] The version of glibc the library was built against.
 */
@Serializable
data class GnuLinuxAbiMetadata(
    val arch: String,
    @SerialName("glibc_version")
    val glibcVersion: String
)