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

package dev.kobalt.hsm2midi.jvm.converter

import dev.kobalt.hsm2midi.jvm.entity.HsmModuleEntity
import javax.sound.midi.MetaMessage
import javax.sound.midi.MidiEvent
import javax.sound.midi.Sequence
import javax.sound.midi.ShortMessage

class Hsm2MidiConverter {

    fun convert(module: HsmModuleEntity, noteOffset: Int, loopCount: Int): Sequence {
        // 960 PPQ should be the default. https://stackoverflow.com/questions/66058516/does-a-midi-files-resolution-affect-its-tempo
        val resolution = 96
        // HSM module uses up to 5 tracks by default.
        val sequence = Sequence(Sequence.PPQ, resolution, 5)
        // Tick counter per track.
        val ticks = mutableListOf(0L, 0L, 0L, 0L, 0L)
        repeat(loopCount) {
            // Use sequenced patterns as they are sorted by song order.
            module.sequencedPatterns.forEach { pattern ->
                // For each track...
                pattern.tracks.forEachIndexed { trackIndex, track ->
                    // Get the matching track.
                    val sequenceTrack = sequence.tracks[trackIndex]
                    // Apply initial track volume.
                    sequenceTrack.add(
                        MidiEvent(
                            ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 7, track.volume),
                            ticks[trackIndex]
                        )
                    )
                    ticks[trackIndex] = ticks[trackIndex] + 1
                    // Apply initial track pan.
                    sequenceTrack.add(
                        MidiEvent(
                            ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 10, track.pan),
                            ticks[trackIndex]
                        )
                    )
                    ticks[trackIndex] = ticks[trackIndex] + 1
                    // Calculate tempo / microseconds per quarter note with beats per minute from pattern.
                    val microsecondsPerMinute = 60_000_000
                    val beatsPerMinute = pattern.bpm
                    val microsecondsPerQuarterNote = microsecondsPerMinute / beatsPerMinute
                    // Apply it as an array of bytes, split into parts.
                    val mpqBytes = byteArrayOf(
                        (microsecondsPerQuarterNote shr 16).toByte(),
                        (microsecondsPerQuarterNote shr 8).toByte(),
                        (microsecondsPerQuarterNote shr 0).toByte(),
                    )
                    // Apply initial track tempo.
                    sequenceTrack.add(MidiEvent(MetaMessage(0x51, mpqBytes, 3), ticks[trackIndex]))
                    ticks[trackIndex] = ticks[trackIndex] + 1
                    // For each step in track, limited up to actual amount of steps in it...
                    track.steps.subList(0, pattern.steps).forEach { step ->
                        // Apply instrument with sample from step.
                        sequenceTrack.add(
                            MidiEvent(
                                ShortMessage(
                                    ShortMessage.PROGRAM_CHANGE,
                                    trackIndex,
                                    step.sample,
                                    0
                                ), ticks[trackIndex]
                            )
                        )
                        ticks[trackIndex] = ticks[trackIndex] + 1
                        // Apply volume from step.
                        sequenceTrack.add(
                            MidiEvent(
                                ShortMessage(
                                    ShortMessage.CONTROL_CHANGE,
                                    trackIndex,
                                    7,
                                    step.volume
                                ), ticks[trackIndex]
                            )
                        )
                        ticks[trackIndex] = ticks[trackIndex] + 1
                        // Apply pan from step.
                        sequenceTrack.add(
                            MidiEvent(
                                ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 10, step.pan),
                                ticks[trackIndex]
                            )
                        )
                        ticks[trackIndex] = ticks[trackIndex] + 1
                        // TODO: Apply release from step sample?
                        // sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 72, module.samples[step.sample].loop),  ticks[trackIndex]))
                        // ticks[trackIndex] = ticks[trackIndex] + 1
                        // TODO: Apply attack from step sample?
                        // sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 73, (module.samples[step.sample].attack / 32) * 128),  ticks[trackIndex]))
                        // ticks[trackIndex] = ticks[trackIndex] + 1
                        // TODO: Apply sustain from step sample?
                        // sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 64, module.samples[step.sample].sustain),  ticks[trackIndex]))
                        // ticks[trackIndex] = ticks[trackIndex] + 1
                        // TODO: Apply decay from step sample?
                        // sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 80, module.samples[step.sample].decay),  ticks[trackIndex]))
                        // ticks[trackIndex] = ticks[trackIndex] + 1
                        // Try to get tick offset between on and off note events.
                        val noteOffTickOffset = (resolution / 5) // Is this accurate enough?
                        // Velocity value for the note.
                        val noteVelocity = 127 // Max value.
                        // Apply note on event from audible step note with given offset and velocity.
                        if (step.isAudible) sequenceTrack.add(
                            MidiEvent(
                                ShortMessage(
                                    ShortMessage.NOTE_ON,
                                    trackIndex,
                                    step.note + noteOffset,
                                    noteVelocity
                                ), ticks[trackIndex]
                            )
                        )
                        ticks[trackIndex] =
                            ticks[trackIndex] + noteOffTickOffset // Offset needs to be applied to avoid short timing!
                        // Apply note off event from audible step note with given offset and velocity.
                        if (step.isAudible) sequenceTrack.add(
                            MidiEvent(
                                ShortMessage(
                                    ShortMessage.NOTE_OFF,
                                    trackIndex,
                                    step.note + noteOffset,
                                    noteVelocity
                                ), ticks[trackIndex]
                            )
                        )
                        ticks[trackIndex] = ticks[trackIndex] + 1
                    }
                }
            }
        }
        return sequence
    }
}
