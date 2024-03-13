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

package dev.kobalt.hsm2midi.jvm.player

import dev.kobalt.hsm2midi.jvm.entity.HsmModuleEntity
import java.io.File
import javax.sound.midi.*

// LEGACY, NO LONGER IN USE.
class Hsm2MidiPlayer(
    private val noteOffset: Int,
    private val loopCount: Int
) {

    fun play(module: HsmModuleEntity) {
        // 960 PPQ should be the default. https://stackoverflow.com/questions/66058516/does-a-midi-files-resolution-affect-its-tempo
        val resolution = 96
        // HSM module uses up to 5 tracks by default.
        val sequence = Sequence(Sequence.PPQ, resolution, 5)
        // Tick counter per track.
        val ticks = mutableListOf(0L, 0L, 0L, 0L, 0L)
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
                track.steps.subList(0, pattern.steps).forEachIndexed { index, step ->
                    // Apply instrument with sample from step.
                    sequenceTrack.add(
                        MidiEvent(
                            ShortMessage(
                                ShortMessage.PROGRAM_CHANGE,
                                trackIndex,
                                10 /*step.sample*/,
                                0
                            ), ticks[trackIndex]
                        )
                    )
                    ticks[trackIndex] = ticks[trackIndex] + 1
                    // Apply volume from step.
                    sequenceTrack.add(
                        MidiEvent(
                            ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 7, step.volume),
                            ticks[trackIndex]
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
        MidiSystem.write(sequence, 1, File("test.mid"))
    }

    fun playOK(module: HsmModuleEntity) {
        val resolution =
            96 // Should be standard? https://stackoverflow.com/questions/66058516/does-a-midi-files-resolution-affect-its-tempo
        val sequence = Sequence(Sequence.PPQ, resolution, 5)

        var ticks = mutableListOf(0L, 0L, 0L, 0L, 0L)

        module.sequencedPatterns.forEach { pattern ->
            pattern.tracks.forEachIndexed { trackIndex, track ->
                val sequenceTrack = sequence.tracks[trackIndex]
                sequenceTrack.add(
                    MidiEvent(
                        ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 7, track.volume),
                        ticks[trackIndex]
                    )
                )
                ticks[trackIndex] = ticks[trackIndex] + 1

                sequenceTrack.add(
                    MidiEvent(
                        ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 10, track.pan),
                        ticks[trackIndex]
                    )
                )
                ticks[trackIndex] = ticks[trackIndex] + 1

                val microsecondsPerMinute = 60_000_000
                val beatsPerMinute = pattern.bpm
                val microsecondsPerQuarterNote = microsecondsPerMinute / beatsPerMinute
                val mpqBytes = byteArrayOf(
                    (microsecondsPerQuarterNote shr 16).toByte(),
                    (microsecondsPerQuarterNote shr 8).toByte(),
                    (microsecondsPerQuarterNote shr 0).toByte(),
                )
                // SET tempo
                sequenceTrack.add(MidiEvent(MetaMessage(0x51, mpqBytes, 3), ticks[trackIndex]))
                ticks[trackIndex] = ticks[trackIndex] + 1

                track.steps.subList(0, pattern.steps).forEachIndexed { index, step ->
                    sequenceTrack.add(
                        MidiEvent(
                            ShortMessage(
                                ShortMessage.PROGRAM_CHANGE,
                                trackIndex,
                                10 /*step.sample*/,
                                0
                            ), ticks[trackIndex]
                        )
                    )
                    ticks[trackIndex] = ticks[trackIndex] + 1
                    sequenceTrack.add(
                        MidiEvent(
                            ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 7, step.volume),
                            ticks[trackIndex]
                        )
                    )
                    ticks[trackIndex] = ticks[trackIndex] + 1

                    sequenceTrack.add(
                        MidiEvent(
                            ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 10, step.pan),
                            ticks[trackIndex]
                        )
                    )
                    ticks[trackIndex] = ticks[trackIndex] + 1
                    // TODO: Sustain??
                    //sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 64, module.samples[step.sample].sustain),  ticks[trackIndex]))
                    //ticks[trackIndex] = ticks[trackIndex] + 1
                    // TODO: Decay??
                    //sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 80, module.samples[step.sample].decay),  ticks[trackIndex]))
                    //ticks[trackIndex] = ticks[trackIndex] + 1
                    // TODO: Release?
                    //sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 72, module.samples[step.sample].loop),  ticks[trackIndex]))
                    //ticks[trackIndex] = ticks[trackIndex] + 1
                    // TODO: Attack
                    //sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 73, (module.samples[step.sample].attack / 32) * 128),  ticks[trackIndex]))
                    //ticks[trackIndex] = ticks[trackIndex] + 1

                    val noteOffTickOffset = (resolution / 5) // This seems accurate?
                    if (step.isAudible) {
                        sequenceTrack.add(
                            MidiEvent(
                                ShortMessage(
                                    ShortMessage.NOTE_ON,
                                    trackIndex,
                                    step.note + noteOffset,
                                    127
                                ), ticks[trackIndex]
                            )
                        )
                        ticks[trackIndex] = ticks[trackIndex] + noteOffTickOffset //+ 45 - 3 // TODO: Figure timing.
                        sequenceTrack.add(
                            MidiEvent(
                                ShortMessage(
                                    ShortMessage.NOTE_OFF,
                                    trackIndex,
                                    step.note + noteOffset,
                                    127
                                ), ticks[trackIndex]
                            )
                        )
                        ticks[trackIndex] = ticks[trackIndex] + 1
                    } else {
                        ticks[trackIndex] = ticks[trackIndex] + noteOffTickOffset //+ 45 - 3
                        ticks[trackIndex] = ticks[trackIndex] + 1
                    }
                }
            }
        }
        MidiSystem.write(sequence, 1, File("test.mid"))
    }

    // This is currently functional without extra features like pan.
    /*fun playOk(module: HsmModuleEntity) {
        val sequence = Sequence(Sequence.PPQ, 192, 5)

        var ticks = mutableListOf(0L,0L,0L,0L,0L)

        module.sequencedPatterns.forEach { pattern ->
            pattern.tracks.forEachIndexed { trackIndex, track ->
                val sequenceTrack = sequence.tracks[trackIndex]
                sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 7, track.volume), ticks[trackIndex]))
                ticks[trackIndex] = ticks[trackIndex] + 1

                val microsecondsPerMinute = 60_000_000
                val beatsPerMinute = pattern.bpm
                val microsecondsPerQuarterNote = microsecondsPerMinute / beatsPerMinute
                val mpqBytes = byteArrayOf(
                    (microsecondsPerQuarterNote shr 16).toByte(),
                    (microsecondsPerQuarterNote shr 8).toByte(),
                    (microsecondsPerQuarterNote shr 0).toByte(),
                )
                // SET tempo
                sequenceTrack.add(MidiEvent(MetaMessage(0x51, mpqBytes, 3),  ticks[trackIndex]))
                ticks[trackIndex] = ticks[trackIndex] + 1

                track.steps.subList(0, pattern.steps).forEachIndexed { index, step ->
                    sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.PROGRAM_CHANGE, trackIndex, 30 /* step.sample */, 0), ticks[trackIndex]))
                    ticks[trackIndex] = ticks[trackIndex] + 1
                    sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 7, step.volume),  ticks[trackIndex]))
                    ticks[trackIndex] = ticks[trackIndex] + 1
                    if (step.isAudible) {
                        sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.NOTE_ON, trackIndex, step.note + noteOffset, pattern.bpmDelayValue),  ticks[trackIndex]))
                        ticks[trackIndex] = ticks[trackIndex] + 45 - 3 // TODO: Figure timing.
                        sequenceTrack.add(MidiEvent(ShortMessage(ShortMessage.NOTE_OFF, trackIndex, step.note + noteOffset,  pattern.bpmDelayValue),  ticks[trackIndex]))
                        ticks[trackIndex] = ticks[trackIndex] + 1
                    } else {
                        ticks[trackIndex] = ticks[trackIndex] + 45 - 3
                        ticks[trackIndex] = ticks[trackIndex] + 1
                    }
                }
            }
        }
        MidiSystem.write(sequence, 1, File("test.mid"))
    }

    fun addNote(track: Track, startTick: Int, tickLength: Int, key: Int, velocity: Int) {
        val on = ShortMessage()
        on.setMessage(ShortMessage.NOTE_ON, 0, key, velocity)
        val off = ShortMessage()
        off.setMessage(ShortMessage.NOTE_OFF, 0, key, velocity)
        track.add(MidiEvent(on, startTick.toLong()))
        track.add(MidiEvent(off, (startTick + tickLength).toLong()))
    }

    fun play222(module: HsmModuleEntity) {
        // TODO: Use sequencer to generate notes to file somehow!
        MidiSystem.getSequencer().also { sequencer ->
        MidiSystem.getSynthesizer().also { synthesizer ->
            sequencer.open()
            synthesizer.open()

            val sequenceTest = MidiSystem.getSequence(File("D:\\Games\\DOSBox\\MISC\\WIN31EXP\\WEP\\CHIP02.MID"))
            val sequenceTest2 = MidiSystem.getSequence(File("test.mid"))
           /* val sequencerTrans = sequencer.transmitter;
            val synthesizerRcvr = synthesizer.receiver;
            sequencerTrans.receiver = synthesizerRcvr;*/
            val sequence = Sequence(Sequence.PPQ, 5)

            val sequenceTracks = (0 until 16).map { sequence.createTrack() }

            val instruments = module.samples.mapIndexedNotNull { index, _ ->
                synthesizer.defaultSoundbank.instruments[instrumentNumbers[index]]
            }

            instruments.forEach { synthesizer.loadInstrument(it) }
            // TODO: FIGURE THIS OUT.
            repeat(10) {
                module.sequencedPatterns.forEach { pattern ->
                    (0 until pattern.steps).forEach { stepIndex ->
                        pattern.tracks.forEachIndexed { trackIndex, track ->
                            track.steps[stepIndex].takeIf { it.isAudible }?.also {
                                /*synthesizer.channels[trackIndex].apply {
                                    programChange(instruments[it.sample].patch.program)
                                    controlChange(7, it.volume)
                                    noteOn(it.note + noteOffset, pattern.bpmDelayValue)
                                }*/
                                val index = (trackIndex * stepIndex * 2).toLong()
                                //sequenceTracks[trackIndex].add(MidiEvent(ShortMessage(ShortMessage.PROGRAM_CHANGE, trackIndex, instruments[it.sample].patch.program,), index.toLong()))
                                //sequenceTracks[trackIndex].add(MidiEvent(ShortMessage(ShortMessage.CONTROL_CHANGE, trackIndex, 7, it.volume), index.toLong() + 1))
                                sequenceTracks[trackIndex].add(MidiEvent(ShortMessage(ShortMessage.NOTE_ON, trackIndex, it.note + noteOffset, pattern.bpmDelayValue), index))
                                sequenceTracks[trackIndex].add(MidiEvent(ShortMessage(ShortMessage.NOTE_OFF, trackIndex, it.note + noteOffset, pattern.bpmDelayValue), index + 1))



                            }
                        }
                        //Thread.sleep(pattern.bpmDelayValue.toLong())
                        pattern.tracks.forEachIndexed { trackIndex, track ->
                            track.steps[stepIndex].takeIf { it.isAudible }?.also {
                                //synthesizer.channels[trackIndex].noteOff(it.note + noteOffset, pattern.bpmDelayValue)
                            }
                        }
                    }
                }
            }

            sequencer.stop()
            sequencer.tickPosition = 0
            sequencer.sequence = sequence
            sequencer.start()
           //Thread.sleep(5000)

            MidiSystem.write(sequence, 1, File("test.mid"))
            sequencer.close()
            synthesizer.close()
        }
    }
        }*/

    /*fun play(module: HsmModuleEntity) {
         val player = Player()
         /*val vocals = Pattern()
         vocals.add("I[TROMBONE] Rh G5is E5i Ri | G5s Ris E5q Rs | G5q E5i Rs D5q rs C5h Rs")
         vocals.add("I[ALTO_SAX] C4i A5q G5isa50d0 Rs A5s E5i D5is Rs C5qis")
         vocals.add("I[TROMBONE] Rqi A4s G5i E5i Rs | G5is Rs E5q | D5is C5i Rs C5q G4q Ri")
         vocals.add("I[TRUMPET] G3is A3s C4is D4s C4is D4s G4is A4s G4is A4s | E4q rs F4h")
         vocals.add("I[TROMBONE] G5is E5i Ri | G5s Ris E5q Rs | G5q E5i Rs A5is rs G5q A5s E5i D5i ri C5h Ri")
         vocals.add("C5s A3q C5i Rs | D5i Rs Eb5qs Rs | D5q Eb5i Rs D5is Eb5s D4q Rs | C5i A4q C5h")
         vocals.setTempo(180)
         player.play(vocals)*/

         module.sequencedPatterns.forEach { pattern ->
             var main = ""
             (0 until pattern.steps).forEach { stepIndex ->
                 pattern.tracks.forEachIndexed { trackIndex, track ->
                     main += "V$trackIndex T${pattern.bpm * 2} "
                     track.steps[stepIndex].takeIf { it.isAudible }?.also {
                         main += it.sample.toStaccatoInstrumentString() + " " + it.note.toStaccatoNoteString() + " "
                     }
                 }
             }
             player.play(main)
             /*val patterns = pattern.tracks.mapIndexed { index, track ->
                 "V$index T${pattern.bpm * 5} " + track.steps.joinToString(" ") { it.sample.toStaccatoInstrumentString() + " " + it.note.toStaccatoNoteString()} + " "
             }

             player.play(*patterns.toTypedArray())*/
         }

     }*/

    /* fun playOld(module: HsmModuleEntity) {
         // TODO: Use sequencer to generate notes to file somehow!
         MidiSystem.getSynthesizer().also { synthesizer ->
             synthesizer.open()
             val instruments = module.samples.mapIndexedNotNull { index, _ ->
                 synthesizer.defaultSoundbank.instruments[instrumentNumbers[index]]
             }
             instruments.forEach { synthesizer.loadInstrument(it) }

             repeat(loopCount) {
                 module.sequencedPatterns.forEach { pattern ->
                     (0 until pattern.steps).forEach { stepIndex ->
                         pattern.tracks.forEachIndexed { trackIndex, track ->
                             track.steps[stepIndex].takeIf { it.isAudible }?.also {
                                 synthesizer.channels[trackIndex].apply {
                                     programChange(instruments[it.sample].patch.program)
                                     controlChange(7, it.volume)
                                     noteOn(it.note + noteOffset, pattern.bpmDelayValue)
                                 }
                             }
                         }
                         Thread.sleep(pattern.bpmDelayValue.toLong())
                         pattern.tracks.forEachIndexed { trackIndex, track ->
                             track.steps[stepIndex].takeIf { it.isAudible }?.also {
                                 synthesizer.channels[trackIndex].noteOff(it.note + noteOffset, pattern.bpmDelayValue)
                             }
                         }
                     }
                 }
             }
         }
     }*/

}

/*
fun Int.toStaccatoInstrumentString() = "I[${when (this) {
    0 -> "PIANO"
    else -> "PIANO"
}}]"

fun Int.toStaccatoNoteString() = when (this) {
    0 -> "C1"
    1 -> "D1"
    2 -> "E1"
    3 -> "F1"
    4 -> "G1"
    5 -> "A1"
    6 -> "B1"
    7 ->  "C2"
    8 ->  "D2"
    9 ->  "E2"
    10 -> "F2"
    11 -> "G2"
    12 -> "A2"
    13 -> "B2"
    14 -> "C3"
    15 -> "D3"
    16 -> "E3"
    17 -> "F3"
    18 -> "G3"
    19 -> "A3"
    20 -> "B3"
    21 -> "C4"
    22 -> "D4"
    23 -> "E4"
    24 -> "F4"
    25 -> "G4"
    26 -> "A4"
    27 -> "B4"
    28 -> "C5"
    29 -> "D5"
    30 -> "E5"
    31 -> "F5"
    32 -> "G5"
    33 -> "A5"
    34 -> "B5"
    35 -> "C6"
    36 -> "D6"
    37 -> "E6"
    38 -> "F6"
    39 -> "G6"
    40 -> "A6"
    41 -> "B6"
    42 -> "C7"
    43 -> "D7"
    44 -> "E7"
    45 -> "F7"
    46 -> "G7"
    47 -> "A7"
    48 -> "B7"
    49 -> "C8"
    50 -> "D8"
    51 -> "E8"
    52 -> "F8"
    53 -> "G8"
    54 -> "A8"
    55 -> "B8"
    56 -> "C1"
    57 -> "D1"
    58 -> "E1"
    59 -> "F1"
    else -> "R"
}*/

