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

package dev.kobalt.hsm2midi.jvm

import dev.kobalt.hsm2midi.jvm.converter.Hsm2MidiConverter
import dev.kobalt.hsm2midi.jvm.extension.ArgTypeFile
import dev.kobalt.hsm2midi.jvm.parser.Hsm2MidiParser
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import java.io.BufferedInputStream
import java.io.File
import javax.sound.midi.MidiSystem

fun main(args: Array<String>) {
    val argParser = ArgParser("hsm2midi")
    val modulePath by argParser.option(
        ArgType.String,
        "modulePath",
        "Module file path",
        "Location of the file that will be converted."
    )
    val noteOffset by argParser.option(
        ArgType.Int,
        "noteOffset",
        "Note offset",
        "Offset amount of notes to increase or decrease octave. Default value is 36."
    )
    val loopCount by argParser.option(
        ArgType.Int,
        "loopCount",
        "Loop count",
        "Amount of loops to be created. Default value is 1."
    )
    val outputPath by argParser.option(
        ArgTypeFile,
        "outputPath",
        "Output file path",
        "Location where the file will be saved."
    )
    argParser.parse(args)
    val outputStream = outputPath?.outputStream()
    val parser = Hsm2MidiParser()
    val converter = Hsm2MidiConverter()
    ((modulePath?.let { File(it).readText() }) ?: run {
        BufferedInputStream(System.`in`).use { if (it.available() > 0) it.readBytes().decodeToString() else null }
    })?.let {
        parser.parse(it)
    }?.let {
        converter.convert(it, noteOffset ?: 36, loopCount ?: 1)
    }?.let {
        MidiSystem.write(it, 1, outputStream ?: System.out)
    }
}