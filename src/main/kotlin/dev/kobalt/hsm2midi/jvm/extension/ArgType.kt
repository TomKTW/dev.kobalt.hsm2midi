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

import kotlinx.cli.ArgType
import java.io.File

object ArgTypeFile : ArgType<File>(true) {
    override val description: kotlin.String get() = "{ File }"

    override fun convert(value: kotlin.String, name: kotlin.String): File = value.toFile()
}