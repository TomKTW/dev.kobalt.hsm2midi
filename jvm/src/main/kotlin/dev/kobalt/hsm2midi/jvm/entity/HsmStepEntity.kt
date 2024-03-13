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

package dev.kobalt.hsm2midi.jvm.entity

class HsmStepEntity(
    val sample: Int,
    val note: Int,
    val pan: Int,
    val volume: Int,
    val echo: Int,
    val echoAmt: Int,
    val vibrato: Int,
    val vibAmt: Int,
    val tremolo: Int,
    val tremAmt: Int,
    val offset: Int,
    val blank: String,
    val arpegSteps: Int,
    val arpegSequence: Int,
    val pitchSteps: Int,
    val pitchNotes: Int,
    val skipSteps: Int
) {

    companion object {
        val sampleRange = (1..26)
        val noteRange = (0..59)
    }

    val isAudible get() = (sample in sampleRange && note in noteRange)

}