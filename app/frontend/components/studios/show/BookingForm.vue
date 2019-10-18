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
    <form class="grid-y">
        <div class="cell small-12">
            <booking-times-form
                :current-time="currentTime"
                :opening-schedule='openingSchedule'
                :occupancies="occupancies"
                :min-booking-duration="minBookingDuration">
            </booking-times-form>
        </div>

        <div class="cell small-12">
            <button
                type="submit"
                class="button primary large expanded">
                Book now
            </button>
        </div>
    </form>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import * as moment from 'moment';

import BookingTimesForm from '../BookingTimesForm.vue';

export default Vue.extend({
    props: {
        // The current local time, without timezone.
        currentTime: { type: String, required: true },

        // An array of {is-open, opens-at, closes-at} 7 objects. Starts on Monday.
        openingSchedule: <PropOptions<Object[]>>{ type: Array, required: true },

        // A list of {starts-at, ends-at} ISO 8601 local date times that define
        // unavailable time periods.
        occupancies: <PropOptions<Object[]>>{ type: Array, required: true },

        // The minimum duration of a booking, in seconds.
        minBookingDuration: { type: Number, required: true },
    },
    data() {
        return {
        }
    },
    computed: {

    },
    components: { BookingTimesForm }
});
</script>

<style>
</style>
