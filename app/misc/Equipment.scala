/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019  Raphael Javaux <raphaeljavaux@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package misc

import scala.language.implicitConversions

object EquipmentFamily extends Enumeration {

    case class EquipmentFamilyVal(val code: String, val name: String) extends super.Val

    implicit def valueToEquipmentFamilyVal(v: Value): EquipmentFamilyVal =
        v.asInstanceOf[EquipmentFamilyVal]

    val AudioSystem = EquipmentFamilyVal("audio-system", "Audio system")
    val Brass = EquipmentFamilyVal("percussion", "Percussion")
    val Keyboard = EquipmentFamilyVal("keyboard", "Keyboard")
    val Percussion = EquipmentFamilyVal("percussion", "Percussion")
    val String = EquipmentFamilyVal("string", "String")

    def byCode: Map[String, EquipmentFamilyVal] = values.
        toSeq.
        map(_.asInstanceOf[EquipmentFamilyVal]).
        map { v => v.code -> v }.
        toMap
}

object EquipmentCategory extends Enumeration {

    case class EquipmentCategoryVal(
        val code: String, val name: String, val family: EquipmentFamily.EquipmentFamilyVal)
        extends super.Val

    implicit def valueToEquipmentCategoryVal(v: Value): EquipmentCategoryVal =
        v.asInstanceOf[EquipmentCategoryVal]

    val Cymbal = EquipmentCategoryVal("cymbal", "Cymbal", EquipmentFamily.Percussion)
    val DrumKit = EquipmentCategoryVal("drum-kit", "Drum kit", EquipmentFamily.Percussion)
    val Percussion = EquipmentCategoryVal("percussion", "Percussion", EquipmentFamily.Percussion)

    val AcousticBass = EquipmentCategoryVal(
        "accoustic-bass", "Accoustic bass guitar", EquipmentFamily.String)
    val ElectricBass = EquipmentCategoryVal(
        "electric-bass", "Electic bass guitar", EquipmentFamily.String)
    val BassAmplifier = EquipmentCategoryVal(
        "bass-amplifier", "Bass amplifier", EquipmentFamily.String)

    val AcousticGuitar = EquipmentCategoryVal(
        "accoustic-guitar", "Accoustic guitar", EquipmentFamily.String)
    val ElectricGuitar = EquipmentCategoryVal(
        "electric-guitar", "Electic guitar", EquipmentFamily.String)
    val GuitarAmplifier = EquipmentCategoryVal(
        "guitar-amplifier", "Guitar amplifier", EquipmentFamily.String)

    val DoubleBass = EquipmentCategoryVal("double-bass", "Double bass", EquipmentFamily.String)
    val Cello = EquipmentCategoryVal("cello", "Cello", EquipmentFamily.String)
    val Violin = EquipmentCategoryVal("violin", "Violin", EquipmentFamily.String)
    val Viola = EquipmentCategoryVal("viola", "Viola", EquipmentFamily.String)

    val Piano = EquipmentCategoryVal("piano", "Piano", EquipmentFamily.Keyboard)
    val Keyboard = EquipmentCategoryVal("keyboard", "Keyboard", EquipmentFamily.Keyboard)
    val Organ = EquipmentCategoryVal("organ", "Organ", EquipmentFamily.Keyboard)
    val Synthesizer = EquipmentCategoryVal("synthesizer", "Synthesizer", EquipmentFamily.Keyboard)

    val Microphone = EquipmentCategoryVal("microphone", "Microphone", EquipmentFamily.AudioSystem)
    val Mixer = EquipmentCategoryVal("mixer", "Mixer", EquipmentFamily.AudioSystem)
    val PASystem = EquipmentCategoryVal("pa-system", "PA system", EquipmentFamily.AudioSystem)
    val Headphones = EquipmentCategoryVal("headphones", "Headphones", EquipmentFamily.AudioSystem)

    def byCode: Map[String, EquipmentCategoryVal] = values.
        toSeq.
        map(_.asInstanceOf[EquipmentCategoryVal]).
        map { v => v.code -> v }.
        toMap
}
