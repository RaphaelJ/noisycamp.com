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
        method="get">

        <div class="cell small-12">
            <booking-times-form
                :current-time="currentTime"
                :opening-schedule='openingSchedule'
                :occupancies="occupancies"
                :min-booking-duration="minBookingDuration"
                v-model="bookingTimes">
            </booking-times-form>
        </div>

        <transition name="slide">
            <div class="cell small-12" v-if="canComputePricing">
                <hr>

                <booking-pricing-calculator
                    :opening-schedule='openingSchedule'
                    :pricing-policy="pricingPolicy"
                    :booking-times="bookingTimes">
                </booking-pricing-calculator>

                <hr>
            </div>
        </transition>

        <div class="cell small-12">
            <button
                type="submit"
                class="button primary large expanded"
                @click="bookNow()">
                Book now
            </button>

            <p class="help-text text-center">You will not be charged now</p>
        </div>
    </form>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import * as moment from 'moment';

import BookingPricingCalculator from '../BookingPricingCalculator.vue';
import BookingTimesForm from '../BookingTimesForm.vue';

declare var NC_ROUTES: any;

export default Vue.extend({
    props: {
        // The current local time, without timezone.
        currentTime: { type: String, required: true },

        studioId: { type: Number, required: true },

        // An array of {is-open, opens-at, closes-at} 7 objects. Starts on Monday.
        openingSchedule: <PropOptions<Object[]>>{ type: Array, required: true },

        // A list of {starts-at, ends-at} ISO 8601 local date times that define
        // unavailable time periods.
        occupancies: <PropOptions<Object[]>>{ type: Array, required: true },

        // The minimum duration of a booking, in seconds.
        minBookingDuration: { type: Number, required: true },

        pricingPolicy: { type: Object, required: true },
    },
    data() {
        return {
            bookingTimes: {
                date: null,
                time: null,
                duration: null,
            },
        }
    },
    computed: {
        bookingUrl() {
            let url = NC_ROUTES.controllers.studios.Booking.show(this.studioId).url;
            // Removes any unspecified URL parameter.
            return url.substring(0, url.indexOf('?'));
        },

        canComputePricing() {
            return this.bookingTimes.date
                && this.bookingTimes.time
                && this.bookingTimes.duration;
        },
    },
    components: { BookingPricingCalculator, BookingTimesForm }
});
</script>

<style>
.booking-form .help-text {
    margin-top: -0.8rem;
}

.booking-form .slide-enter-active,
.booking-form .slide-leave-active {
    transition: max-height 0.15s linear;
    overflow: hidden;
}

.booking-form .slide-enter-to,
.booking-form .slide-leave {
    max-height: 150px;
    opacity: 1;
}

.booking-form .slide-leave-to,
.booking-form .slide-enter {
    max-height: 0;
    opacity: 0;
}

</style>
