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

package daos

import java.time.{ Duration, Instant, LocalTime, ZoneId }
import scala.concurrent.{ ExecutionContext, Future }
import javax.inject.Inject

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile
import squants.market.Currency

import i18n.Country
import models.{ Address, BookingPolicy, CancellationPolicy, Coordinates,
  EveningPricingPolicy, Location, OpeningSchedule, OpeningTimes, PaymentPolicy,
  PricingPolicy, Studio, User, WeekendPricingPolicy }

class StudioDAO @Inject()
  (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

  import profile.api._

  final class StudioTable(tag: Tag) extends Table[Studio](tag, "studio") {

    def id                  = column[Studio#Id]("id", O.PrimaryKey, O.AutoInc)
    def createdAt           = column[Instant]("created_at")
    def ownerId             = column[User#Id]("owner_id")

    def name                = column[String]("name")
    def description         = column[String]("description")

    def address1            = column[String]("address1")
    def address2            = column[Option[String]]("address2")
    def zipcode             = column[String]("zipcode")
    def city                = column[String]("city")
    def stateCode           = column[Option[String]]("state_code")
    def country             = column[Country.Val]("country_code")

    def long                = column[BigDecimal]("long")
    def lat                 = column[BigDecimal]("lat")

    def timezone            = column[ZoneId]("timezone")

    def mondayIsOpen        = column[Boolean]("monday_is_open")
    def mondayOpensAt       =
      column[Option[LocalTime]]("monday_opens_at")(localTimeType.optionType)
    def mondayClosesAt      =
      column[Option[LocalTime]]("monday_closes_at")(localTimeType.optionType)

    def tuesdayIsOpen       = column[Boolean]("tuesday_is_open")
    def tuesdayOpensAt      =
      column[Option[LocalTime]]("tuesday_opens_at")(localTimeType.optionType)
    def tuesdayClosesAt     =
      column[Option[LocalTime]]("tuesday_closes_at")(localTimeType.optionType)

    def wednesdayIsOpen     = column[Boolean]("wednesday_is_open")
    def wednesdayOpensAt    =
      column[Option[LocalTime]]("wednesday_opens_at")(localTimeType.optionType)
    def wednesdayClosesAt   =
      column[Option[LocalTime]]("wednesday_closes_at")(localTimeType.optionType)

    def thursdayIsOpen      = column[Boolean]("thursday_is_open")
    def thursdayOpensAt     =
      column[Option[LocalTime]]("thursday_opens_at")(localTimeType.optionType)
    def thursdayClosesAt    =
      column[Option[LocalTime]]("thursday_closes_at")(localTimeType.optionType)

    def fridayIsOpen        = column[Boolean]("friday_is_open")
    def fridayOpensAt       =
      column[Option[LocalTime]]("friday_opens_at")(localTimeType.optionType)
    def fridayClosesAt      =
      column[Option[LocalTime]]("friday_closes_at")(localTimeType.optionType)

    def saturdayIsOpen      = column[Boolean]("saturday_is_open")
    def saturdayOpensAt     =
      column[Option[LocalTime]]("saturday_opens_at")(localTimeType.optionType)
    def saturdayClosesAt    =
      column[Option[LocalTime]]("saturday_closes_at")(localTimeType.optionType)

    def sundayIsOpen        = column[Boolean]("sunday_is_open")
    def sundayOpensAt       =
      column[Option[LocalTime]]("sunday_opens_at")(localTimeType.optionType)
    def sundayClosesAt      =
      column[Option[LocalTime]]("sunday_closes_at")(localTimeType.optionType)

    def pricePerHour        = column[BigDecimal]("price_per_hour")

    def hasEveningPricing   = column[Boolean]("has_evening_pricing")
    def eveningBeginsAt     =
      column[Option[LocalTime]]("evening_begins_at")(localTimeType.optionType)
    def eveningPricePerHour =
      column[Option[BigDecimal]]("evening_price_per_hour")

    def hasWeekendPricing   = column[Boolean]("has_weekend_pricing")
    def weekendPricePerHour =
      column[Option[BigDecimal]]("weekend_price_per_hour")

    def minBookingDuration  = column[Duration]("min_booking_duration")
    def automaticApproval   = column[Boolean]("automatic_approval")

    def canCancel           = column[Boolean]("can_cancel")
    def cancellationNotice  = column[Option[Duration]]("cancellation_notice")

    def hasOnlinePayment    = column[Boolean]("has_online_payment")
    def hasOnsitePayment    = column[Boolean]("has_onsite_payment")

    private type StudioTuple = (
      Studio#Id, Instant, User#Id, String, String,
      LocationTuple, ZoneId, OpeningScheduleTuple, PricingPolicyTuple,
      BookingPolicyTuple, PaymentPolicyTuple)

    private type AddressTuple = (
      String, Option[String], String, String, Option[String], Country.Val)

    private type CoordinatesTuple = (BigDecimal, BigDecimal)

    private type LocationTuple = (AddressTuple, CoordinatesTuple)

    private type OpeningScheduleTuple = (
      OpeningTimesTuple, OpeningTimesTuple, OpeningTimesTuple,
      OpeningTimesTuple, OpeningTimesTuple, OpeningTimesTuple,
      OpeningTimesTuple)

    private type OpeningTimesTuple = (
      Boolean, Option[LocalTime], Option[LocalTime])

    private type PricingPolicyTuple = (
      BigDecimal, Boolean, Option[LocalTime], Option[BigDecimal],
      Boolean, Option[BigDecimal])

    private type BookingPolicyTuple = (
      Duration, Boolean, Boolean, Option[Duration])

    private type PaymentPolicyTuple = (Boolean, Boolean)

    private val studioShaped = (
      id, createdAt, ownerId, name, description, (
        (address1, address2, zipcode, city, stateCode, country),
        (long, lat)),
      timezone, (
        (mondayIsOpen, mondayOpensAt, mondayClosesAt),
        (tuesdayIsOpen, tuesdayOpensAt, tuesdayClosesAt),
        (wednesdayIsOpen, wednesdayOpensAt, wednesdayClosesAt),
        (thursdayIsOpen, thursdayOpensAt, thursdayClosesAt),
        (fridayIsOpen, fridayOpensAt, fridayClosesAt),
        (saturdayIsOpen, saturdayOpensAt, saturdayClosesAt),
        (sundayIsOpen, sundayOpensAt, sundayClosesAt)),
      (pricePerHour, hasEveningPricing, eveningBeginsAt, eveningPricePerHour,
        hasWeekendPricing, weekendPricePerHour),
      (minBookingDuration, automaticApproval, canCancel, cancellationNotice),
      (hasOnlinePayment, hasOnsitePayment)).shaped

    private def toStudio(studioTuple: StudioTuple): Studio = {
      Studio(studioTuple._1, studioTuple._2, studioTuple._3, studioTuple._4,
        studioTuple._5,
        toLocation(studioTuple._6), studioTuple._7,
        toOpeningSchedule(studioTuple._8),
        toPricingPolicy(studioTuple._9),
        toBookingPolicy(studioTuple._10), toPaymentPolicy(studioTuple._11))
    }

    private def fromStudio(studio: Studio): Option[StudioTuple] = {
      Some((studio.id, studio.createdAt, studio.ownerId, studio.name,
        studio.description, fromLocation(studio.location),
        studio.timezone, fromOpeningSchedule(studio.openingSchedule),
        fromPricingPolicy(studio.pricingPolicy),
        fromBookingPolicy(studio.bookingPolicy),
        fromPaymentPolicy(studio.paymentPolicy)))
    }

    private def toLocation(locationTuple: LocationTuple): Location = {
      Location(Address.tupled(locationTuple._1),
        Coordinates.tupled(locationTuple._2))
    }

    private def fromLocation(location: Location): LocationTuple = {
      (Address.unapply(location.address).get,
        Coordinates.unapply(location.coordinates).get)
    }

    private def toOpeningSchedule(scheduleTuple: OpeningScheduleTuple)
      : OpeningSchedule = {

      def toOpeningTimes(timesTuple: OpeningTimesTuple) = {
        if (timesTuple._1) {
          Some(OpeningTimes(timesTuple._2.get, timesTuple._3.get))
        } else {
          None
        }
      }

      OpeningSchedule(
        toOpeningTimes(scheduleTuple._1), toOpeningTimes(scheduleTuple._2),
        toOpeningTimes(scheduleTuple._3), toOpeningTimes(scheduleTuple._4),
        toOpeningTimes(scheduleTuple._5), toOpeningTimes(scheduleTuple._6),
        toOpeningTimes(scheduleTuple._7))
    }

    private def fromOpeningSchedule(schedule: OpeningSchedule) = {
      def fromOpeningTimes(times: Option[OpeningTimes]) = {
        (times.isDefined, times.map(_.opensAt), times.map(_.closesAt))
      }

      (
        fromOpeningTimes(schedule.monday),
        fromOpeningTimes(schedule.tuesday),
        fromOpeningTimes(schedule.wednesday),
        fromOpeningTimes(schedule.thursday),
        fromOpeningTimes(schedule.friday),
        fromOpeningTimes(schedule.saturday),
        fromOpeningTimes(schedule.sunday))
    }

    private def toPricingPolicy(policyTuple: PricingPolicyTuple) = {
      val evening =
        if (policyTuple._2) {
          Some(EveningPricingPolicy(policyTuple._3.get, policyTuple._4.get))
        } else {
          None
        }

      val weekend = policyTuple._6.map(WeekendPricingPolicy)

      PricingPolicy(policyTuple._1, evening, weekend)
    }

    private def fromPricingPolicy(policy: PricingPolicy) = {
      val evening = policy.evening
      val weekend = policy.weekend

      (
        policy.pricePerHour, evening.isDefined,
        evening.map(_.beginsAt), evening.map(_.pricePerHour),
        weekend.isDefined, weekend.map(_.pricePerHour))
    }

    private def toBookingPolicy(policyTuple: BookingPolicyTuple) = {
      BookingPolicy(policyTuple._1, policyTuple._2,
        policyTuple._4.map(CancellationPolicy))
    }

    private def fromBookingPolicy(policy: BookingPolicy) = {
      (policy.minBookingDuration, policy.automaticApproval,
        policy.cancellationPolicy.isDefined,
        policy.cancellationPolicy.map(_.notice))
    }

    private def toPaymentPolicy(policyTuple: PaymentPolicyTuple) = {
      PaymentPolicy(policyTuple._1, policyTuple._2)
    }

    private def fromPaymentPolicy(policy: PaymentPolicy) = {
      (policy.hasOnlinePayment, policy.hasOnsitePayment)
    }

    def * = studioShaped <> (toStudio, fromStudio)
  }

  lazy val query = TableQuery[StudioTable]

  /** Inserts a studio and returns the newly created object with its inserted
   * ID. */
  def insert(studio: Studio): DBIO[Studio] = {
    query returning query.map(_.id) into ((s, id) => s.copy(id=id)) += studio
  }
}
