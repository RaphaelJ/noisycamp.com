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
                    v-model="date"
                    @change="valueChanged()"
                    :min="mCurrentTime.format('YYYY-MM-DD')"
                    :max="mMax.format('YYYY-MM-DD')"
                    pattern="[0-9]{4}-[0-9]{2}-[0-9]{2}"
                    placeholder="yyyy-mm-dd"
                    required>
            </label>
        </div>

        <div class="cell medium-12 large-6">
            <label>
                Starting at

                <select
                    v-model="time"
                    :disabled="!date"
                    @change="valueChanged()"
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

        <input
            type="hidden"
            name="beginsAt"
            :value="beginsAtStr">

        <div class="cell medium-12 large-6">
            <label>
                Duration

                <select
                    name="duration"
                    v-model="duration"
                    :disabled="!date || !time"
                    @change="valueChanged()"
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

import {
    dateComponent, renderDuration, timeComponent, withTimeComponent
} from '../../../misc/DateUtils';

export default Vue.extend({
    props: {
        // The current local time, without timezone.
        currentTime: { type: String, required: true },

        // The last bookable day (as a ISO 8601 date string).
        end: { type: String, required: false },

        // An array of {is-open, opens-at, closes-at} 7 objects. Starts on Monday.
        openingSchedule: <PropOptions<Object[]>>{ type: Array, required: true },

        // A list of {begins-at, ends-at} ISO 8601 local date times that define
        // unavailable time periods.
        occupancies: <PropOptions<Object[]>>{ type: Array, required: true },

        // The minimum duration of a booking, in seconds.
        minBookingDuration: { type: Number, required: true },

        // A `begins-at`, `duration` object.
        value: {
            type: Object, required: false,
            default: function() {
                return {
                    'begins-at': null,
                    'duration': null
                }
            }
        }
    },
    data() {
        var date = null;
        var time = null;
        var duration = this.value.duration;

        if (this.value.beginsAt) {
            date = dateComponent(moment(this.value.beginsAt));
            time = timeComponent(moment(this.value.beginsAt));
        }

        return {
            date: null,
            time: null,
            duration: null,
        }
    },
    computed: {
        mCurrentTime() {
            return moment(this.currentTime);
        },

        mEnd() {
            if (this.end) {
                return moment(this.end);
            } else {
                return null;
            }
        },

        // The max date (inclusive) value to be passed to the `<input type="date">` widget.
        mMax() {
            if (this.mEnd) {
                return this.mEnd.clone().subtract(1, 'days');
            } else {
                return null;
            }
        },

        mMinBookingDuration() {
            return moment.duration(this.minBookingDuration, 'seconds');
        },

        mDate() {
            if (this.date) {
                return moment(this.date);
            } else {
                return null;
            }
        },

        // The opening schedule of the currently selected date.
        dateSchedule() {
            if (!this.mDate) {
                return null;
            } else {
                let weekDay = this.mDate.isoWeekday();
                return this.openingSchedule[weekDay - 1];
            }
        },

        // When date is set, return the MomentJS date-time at which the studio
        // opens.
        dateOpensAt() {
            if (!this.mDate) {
                return null;
            }

            let opensAtTime = this.dateSchedule['opens-at'];

            return withTimeComponent(this.mDate, opensAtTime);
        },

        // When date is set, return the MomentJS date-time at which the studio
        // closes.
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
            return this.dateSchedule && !this.dateSchedule['is-open'];
        },

        // List all the selected day's availaible starting times.
        startingTimes() {
            if (!this.dateSchedule || this.isClosed) {
                return null;
            } else {

                // The last time a session can start, considering the minimal
                // booking duration.
                let lastSession = this.dateClosesAt
                    .clone()
                    .subtract(this.minBookingDuration, 'seconds');

                var iTime = this.dateOpensAt.clone();

                if (iTime.isBefore(this.mCurrentTime)) {
                    iTime = this.mCurrentTime;
                }

                // Rounds the opening time to the next upcoming 15-minutes.
                let minutes = iTime.minutes();
                if (minutes % 15) {
                    iTime.add(15 - (minutes % 15), 'minutes');
                }

                let times = [];
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

        // Returns a MomentJS date-time for the selected date and times.
        beginsAt() {
            if (!this.date || !this.time) {
                return null;
            }

            let beginsAt = withTimeComponent(this.mDate, this.time);

            if (beginsAt.isBefore(this.dateOpensAt)) {
                // Session start during the next day's early morning.
                beginsAt.add(1, 'days');
            }

            return beginsAt;
        },

        beginsAtStr() {
            if (this.beginsAt) {
                return this.beginsAt.format('YYYY-MM-DDTHH:mm');
            } else {
                return null;
            }
        },

        // List all selected day's and start time's availaible rental
        // durations.
        durations() {
            if (!this.beginsAt) {
                return null;
            }

            let maxDurationMillis = this.dateClosesAt.diff(this.beginsAt);

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
        // Sets the form values to a valid state
        cleanupValues() {
            if (this.mDate && this.mDate.isBefore(this.mCurrentTime, 'day')) {
                this.date = null;
            }

            if (
                !this.startingTimes
                || !this.startingTimes.find(t => t.value == this.time)
            ) {
                this.time = null;
            }

            if (
                !this.durations
                || !this.durations.find(d => d.value == this.duration)
            ) {
                this.duration = null;
            }
        },

        valueChanged() {
            this.cleanupValues();

            var beginsAt = null;
            if (this.beginsAt) {
                beginsAt = this.beginsAt.format('YYYY-MM-DDTHH:mm:ss.SSS');
            }

            this.$emit('input', {
                'begins-at': beginsAt,
                'duration': this.duration,
            });
        }
    },
});
</script>

<style>
</style>
