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
    <div class="grid-x grid-padding-x">
        <div class="cell small-12">
            <label>
                Date
                <input
                    type="date"
                    name="date"
                    v-model="value.date"
                    @change="updateValue()"
                    :min="mCurrentTime.format('YYYY-MM-DD')"
                    pattern="[0-9]{4}-[0-9]{2}-[0-9]{2}"
                    placeholder="yyyy-mm-dd"
                    required>
            </label>
        </div>

        <div class="cell medium-12 large-6">
            <label>
                Starting at

                <select
                    name="time"
                    v-model="value.time"
                    :disabled="!value.date"
                    @change="updateValue()"
                    required>
                    <option v-if="isClosed" value="" disabled>
                        Closed
                    </option>

                    <option
                        v-for="time in startingTimes"
                        :value="time.value"
                        :key="time.value"
                        :disabled="time.disabled">
                        {{ time.value }}
                    </option>
                </select>
            </label>
        </div>

        <div class="cell medium-12 large-6">
            <label>
                Duration

                <select
                    name="duration"
                    v-model="value.duration"
                    :disabled="!value.date || !value.time"
                    @change="updateValue()"
                    required>
                    <option
                        v-for="d in durations"
                        :key="d.value"
                        :value="d.value">
                        {{ d.title }}
                    </option>
                </select>
            </label>
        </div>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import * as moment from 'moment';

import { renderDuration, withTimeComponent } from '../../../misc/DateUtils';

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

        // A `date`, `time`, `duration` object.
        value: {
            type: Object, required: false,
            default: function() {
                return {
                    date: null,
                    time: null,
                    duration: null
                }
            }
        }
    },
    data() {
        return {
        }
    },
    computed: {
        mCurrentTime() {
            return moment(this.currentTime);
        },

        mMinBookingDuration() {
            return moment.duration(this.minBookingDuration, 'seconds');
        },

        mDate() {
            if (this.value.date) {
                return moment(this.value.date)
            } else {
                return null;
            }
        },

        dateSchedule() {
            if (!this.value.date) {
                return null;
            } else {
                let weekDay = this.mDate.isoWeekday();
                return this.openingSchedule[weekDay - 1];
            }
        },

        // When date is set, return the datetime at which the studio opens.
        dateOpensAt() {
            let opensAtTime = this.dateSchedule['opens-at'];

            let opensAt = withTimeComponent(this.mDate, opensAtTime);

            // Rounds opensAt to the upcoming 15-minutes time.
            let opensAtMin = opensAt.minutes();
            if (opensAtMin % 15) {
                opensAt.add(15 - (opensAtMin % 15), 'minutes');
            }

            return opensAt;
        },

        // When date is set, return the datetime at which the studio opens.
        dateClosesAt() {
            let opensAtTime = this.dateSchedule['opens-at'];
            let closesAtTime = this.dateSchedule['closes-at'];

            // Close time might be on the next day.
            let closesAtDate = closesAtTime < opensAtTime
                ? this.mDate.clone().add(1, 'days')
                : this.mDate;

            return withTimeComponent(closesAtDate, closesAtTime);
        },

        isClosed() {
            return this.value.date && !this.dateSchedule['is-open'];
        },

        // List all the selected day's availaible starting times.
        startingTimes() {
            if (!this.dateSchedule || !this.dateSchedule['is-open']) {
                return null;
            } else {

                // The last time a session can start, considering the minimal
                // booking duration.
                let lastSession = this.dateClosesAt
                    .clone()
                    .subtract(this.minBookingDuration, 'seconds');

                let times = [];
                var iTime = this.dateOpensAt.clone();
                while (iTime.isSameOrBefore(lastSession)) {
                    times.push({
                        value: iTime.format('HH:mm'),
                        disabled: false
                    });
                    iTime.add(15, 'minutes');
                }

                return times;
            }
        },

        // List all selected day's and start time's availaible rental
        // durations.
        durations() {
            if (!this.value.time) {
                return null;
            }

            let sessionBeginsAt = withTimeComponent(
                this.mDate, this.value.time
            );

            if (sessionBeginsAt.isBefore(this.dateOpensAt)) {
                // Session start during the next day's early morning.
                sessionBeginsAt.add(1, 'days');
            }

            let maxDurationMillis = this.dateClosesAt.diff(sessionBeginsAt);

            let durations = [];
            var iDuration = this.mMinBookingDuration.clone();
            while (iDuration.asMilliseconds() <= maxDurationMillis) {
                durations.push({
                    value: iDuration.asSeconds(),
                    title: renderDuration(iDuration, 'minutes'),
                });
                iDuration.add(15, 'minutes');
            }

            return durations;
        }
    },
    methods: {
        cleanupValue() {
            if (this.mDate && this.mDate.isBefore(this.mCurrentTime)) {
                this.value.date = null;
            }

            if (
                !this.startingTimes
                || !this.startingTimes.find(t => t.value == this.value.time)
            ) {
                this.value.time = null;
            }

            if (
                !this.durations
                || !this.durations.find(d => d.value == this.value.duration)
            ) {
                this.value.duration = null;
            }
        },

        updateValue() {
            this.cleanupValue();
            this.$emit('change', this.value)
        }
    },
});
</script>

<style>
</style>
