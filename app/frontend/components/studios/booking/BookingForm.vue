<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2019  Raphael Javaux <raphaeljavaux@gmail.com>

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
-->

<template>
    <form
        class="grid-y booking-form"
        :action="bookingUrl"
        method="get"
        :target="target">

        <div class="cell small-12">
            <booking-times-input
                name="times"
                :current-time="currentTime"
                :end="end"
                :opening-schedule='openingSchedule'
                :occupancies="occupancies"
                :min-booking-duration="minBookingDuration"
                v-model="bookingTimes">
            </booking-times-input>

            <hr>
        </div>

        <slide-down-transition :max-height="150">
            <div class="cell small-12" v-if="hasEquipments && canComputePricing">
                <p>Some equipments may be rented for the duration of your session:</p>

                <booking-equipments-input
                    name="equipments[]"
                    :equipments="equipments"
                    :session-duration="bookingTimes['duration']"
                    v-model="selectedEquipments">
                </booking-equipments-input>

                <hr>
            </div>
        </slide-down-transition>

        <slide-down-transition :max-height="150">
            <div class="cell small-12" v-if="canComputePricing">
                <booking-pricing-calculator
                    :opening-schedule='openingSchedule'
                    :pricing-policy="pricingPolicy"
                    :booking-times="bookingTimes"
                    :equipments="selectedEquipments">
                </booking-pricing-calculator>

                <hr>
            </div>
        </slide-down-transition>

        <div class="cell small-12">
            <button
                type="submit"
                class="button primary large expanded">
                Book now
            </button>

            <p class="help-text text-center">You will not be charged now</p>
        </div>
    </form>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import BookingEquipmentsInput from './BookingEquipmentsInput.vue';
import BookingPricingCalculator from './BookingPricingCalculator.vue';
import BookingTimesInput from './BookingTimesInput.vue';

import SlideDownTransition from '../../../transitions/SlideDownTransition.vue';

declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
        // The current local time, without timezone. This will also be the first bookable date.
        currentTime: { type: String, required: true },

        // The last bookable day (as a ISO 8601 date string).
        end: { type: String, required: false },

        studioId: { type: Number, required: true },

        // An array of {is-open, opens-at, closes-at} 7 objects. Starts on Monday.
        openingSchedule: <PropOptions<Object[]>>{ type: Array, required: true },

        // A list of {starts-at, duration} ISO 8601 local date time and durations that define
        // unavailable time periods for which the studio cannot be booked.
        occupancies: <PropOptions<Object[]>>{ type: Array, required: true },

        // The minimum duration of a booking, in seconds.
        minBookingDuration: { type: Number, required: true },

        pricingPolicy: { type: Object, required: true },

        equipments: <PropOptions<Object[]>>{ type: Array, required: true },

        // An optional target value for the "<form>" element.
        target: { type: String, required: false },
    },
    data() {
        return {
            bookingTimes: {
                'begins-at': null,
                'duration': null,
            },

            selectedEquipments: [],
        }
    },
    computed: {
        bookingUrl() {
            return NC_ROUTES.controllers.studios.BookingController.show(this.studioId).url;
        },

        // Returns true if the studio has at least one billable equipment.
        hasEquipments() {
            return this.equipments.some(e => e.price);
        },

        canComputePricing() {
            return this.bookingTimes['begins-at']
                && this.bookingTimes['duration'];
        },

    },
    components: {
        BookingEquipmentsInput, BookingPricingCalculator, BookingTimesInput,
        SlideDownTransition
    }
});
</script>

<style>
.booking-form .help-text {
    margin-top: -0.8rem;
}
</style>
