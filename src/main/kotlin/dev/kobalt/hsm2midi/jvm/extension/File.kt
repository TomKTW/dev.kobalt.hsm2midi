/*
 * dev.kobalt.hsm2midi
 * Copyright (C) 2023 Tom.K
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.kobalt.hsm2midi.jvm.extension

import java.io.File

internal fun File.isLocatedIn(parent: File) = canonicalFile.toPath().startsWith(parent.canonicalFile.toPath())

internal fun File.requireIsLocatedIn(parent: File) = takeIf { isLocatedIn(parent) }
    ?: throw Exception("File ${this.canonicalPath} is not located in ${parent.canonicalPath}.")