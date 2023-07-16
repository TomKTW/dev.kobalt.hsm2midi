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

package dev.kobalt.hsm2midi.jvm.parser

import dev.kobalt.hsm2midi.jvm.entity.*
import dev.kobalt.hsm2midi.jvm.extension.fromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class Hsm2MidiParser {

    fun parse(content: String): HsmModuleEntity {
        val json = content.fromJsonElement()
        val size = json.jsonObject["size"]!!
        val firstArraySize = size.jsonArray[0].jsonPrimitive.int
        val secondArraySize = size.jsonArray[1].jsonPrimitive.int
        val thirdArraySize = size.jsonArray[2].jsonPrimitive.int
        val data = json.jsonObject["data"]!!.jsonArray.map { firstArray ->
            firstArray.jsonArray.map { secondArray ->
                secondArray.jsonArray.map { thirdArray ->
                    thirdArray.jsonPrimitive.content
                }
            }
        }

        val metadata = HsmMetadataEntity(
            title = data[0][0][0],
            author = data[0][0][1],
            filename = data[0][0][2],
            multisamples = data[0][0][3].toInt(),
            lawbroken = data[0][0][4].toInt(),
            loopCount = data[0][0][5].toInt()
        )

        val samples = (1 until firstArraySize).map { x ->
            HsmSampleEntity(
                filename = data[x][0][0],
                loop = data[x][0][1].toInt(),
                decay = data[x][0][2].toInt(),
                sustain = data[x][0][3].toInt(),
                attack = data[x][0][4].toInt()
            )
        }

        val sequence = (0 until firstArraySize).map { x ->
            data[x][0][10].toInt()
        }

        val patterns = (1 until secondArraySize step 5).map { y ->
            HsmPatternEntity(
                bpm = data[0][y][0].toInt(),
                steps = data[0][y][1].toInt(),
                highlights = data[0][y][2].toInt(),
                color = data[0][y][3].toInt(),
                tracks = (y..y + 4).map { y2 ->
                    HsmTrackEntity(
                        pan = data[0][y2][4].toInt(),
                        volume = data[0][y2][5].toInt(),
                        steps = (1 until firstArraySize).map { x ->
                            HsmStepEntity(
                                sample = data[x][y2][0].toInt(),
                                note = data[x][y2][1].toInt(),
                                pan = data[x][y2][2].toInt(),
                                volume = data[x][y2][3].toInt(),
                                echo = data[x][y2][4].toInt(),
                                echoAmt = data[x][y2][5].toInt(),
                                vibrato = data[x][y2][6].toInt(),
                                vibAmt = data[x][y2][7].toInt(),
                                tremolo = data[x][y2][8].toInt(),
                                tremAmt = data[x][y2][9].toInt(),
                                offset = data[x][y2][10].toInt(),
                                blank = "",
                                // Note: guymastercool_teentopia.hsm has the following fields missing!
                                arpegSteps = runCatching { data[x][y2][12].toInt() }.getOrElse { 0 },
                                arpegSequence = runCatching { data[x][y2][13].toInt() }.getOrElse { 0 },
                                pitchSteps = runCatching { data[x][y2][14].toInt() }.getOrElse { 0 },
                                pitchNotes = runCatching { data[x][y2][15].toInt() }.getOrElse { 0 },
                                skipSteps = runCatching { data[x][y2][16].toInt() }.getOrElse { 0 },
                            )
                        }
                    )
                }
            )
        }

        return HsmModuleEntity(metadata, samples, patterns, sequence, firstArraySize)
    }

}