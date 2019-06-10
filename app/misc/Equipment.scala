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

object EquipmentFamily extends Enumeration {

  case class Val(val code: String, val name: String) extends super.Val

  val AudioSystem = Val("audio-system", "Audio system")
  val Brass = Val("percussion", "Percussion")
  val Keyboard = Val("keyboard", "Keyboard")
  val Percussion = Val("percussion", "Percussion")
  val String = Val("string", "String")

  def byCode: Map[String, Val] = values.
    toSeq.
    map(_.asInstanceOf[Val]).
    map { v => v.code -> v }.
    toMap
}

object Equipment extends Enumeration {

  case class Val(
    val code: String, val name: String, val family: EquipmentFamily.Val)
    extends super.Val

  val Cymbal = Val("cymbal", "Cymbal", EquipmentFamily.Percussion)
  val DrumKit = Val("drum-kit", "Drum kit", EquipmentFamily.Percussion)
  val Percussion = Val("percussion", "Percussion", EquipmentFamily.Percussion)

  val AcousticBass = Val(
    "accoustic-bass", "Accoustic bass guitar", EquipmentFamily.String)
  val ElectricBass = Val(
    "electric-bass", "Electic bass guitar", EquipmentFamily.String)
  val BassAmplifier = Val(
    "bass-amplifier", "Bass amplifier", EquipmentFamily.String)

  val AcousticGuitar = Val(
    "accoustic-guitar", "Accoustic guitar", EquipmentFamily.String)
  val ElectricGuitar = Val(
    "electric-guitar", "Electic guitar", EquipmentFamily.String)
  val GuitarAmplifier = Val(
    "guitar-amplifier", "Guitar amplifier", EquipmentFamily.String)

  val DoubleBass = Val("double-bass", "Double bass", EquipmentFamily.String)
  val Cello = Val("cello", "Cello", EquipmentFamily.String)
  val Violin = Val("violin", "Violin", EquipmentFamily.String)
  val Viola = Val("viola", "Viola", EquipmentFamily.String)

  val Piano = Val("piano", "Piano", EquipmentFamily.Keyboard)
  val Keyboard = Val("keyboard", "Keyboard", EquipmentFamily.Keyboard)
  val Organ = Val("organ", "Organ", EquipmentFamily.Keyboard)
  val Synthesizer = Val("synthesizer", "Synthesizer", EquipmentFamily.Keyboard)

  val Microphone = Val("microphone", "Microphone", EquipmentFamily.AudioSystem)
  val Mixer = Val("mixer", "Mixer", EquipmentFamily.AudioSystem)
  val PASystem = Val("pa-system", "PA system", EquipmentFamily.AudioSystem)
  val Headphones = Val("headphones", "Headphones", EquipmentFamily.AudioSystem)

  def byCode: Map[String, Val] = values.
    toSeq.
    map(_.asInstanceOf[Val]).
    map { v => v.code -> v }.
    toMap
}
