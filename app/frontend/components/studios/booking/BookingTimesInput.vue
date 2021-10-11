<!--
  Noisycamp is a platform for booking music studios.
  Copyright (C) 2019 2021 Raphael Javaux <raphael@noisycamp.com>

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
        <div
            :class="{
                'cell': true,
                'small-12': true,
                'large-6': compact
            }">
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

                <span v-if="errors['begins-at']" class="error">
                    {{ errors['begins-at'] }}
                </span>
            </label>
        </div>

        <div
            :class="{
                'cell': true,
                'small-12': true,
                'medium-6': compact,
                'large-6': !compact,
                'large-3': compact
            }">

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
            :name="fieldName('begins-at')"
            :value="beginsAtStr">

        <div
            :class="{
                'cell': true,
                'small-12': true,
                'medium-6': compact,
                'large-6': !compact,
                'large-3': compact
            }">

            <label>
                Duration

                <select
                    :name="fieldName('duration')"
                    v-model="duration"
                    :disabled="!date || !time || isClosed"
                    @change="valueChanged()"
                    required>
                    <option
                        v-for="d in durations"
                        :key="d.value"
                        :value="d.value">
                        {{ d.title }}
                    </option>
                </select>

                <span v-if="errors['duration']" class="error">
                    {{ errors['duration'] }}
                </span>
            </label>
        </div>

        <div
            class="cell small-12"
            v-if="globalError">
            <label>
                <span class="error">{{ globalError }}</span>
            </label>
        </div>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import * as moment from 'moment';

import VueInput from '../../widgets/VueInput';
import {
    dateComponent, eventsOverlap, renderDuration, timeComponent, withTimeComponent
} from '../../../misc/DateUtils';

declare var NC_CONFIG: any;

export default Vue.extend({
    mixins: [VueInput],
    props: {
        // The current local time (as a ISO 8601 date string without timezone).
        currentTime: { type: String, required: true },

        // The last bookable day (exclusive, as a ISO 8601 date string without timezone).
        end: { type: String, required: false },

        // An array of {is-open, opens-at, closes-at} 7 objects. Starts on Monday.
        openingSchedule: <PropOptions<Object[]>>{
            type: Array,
            default() {
                // By default, open all the time.
                return Array(7).fill({
                    'is-open': true, 'opens-at': '00:00:00', 'closes-at': '00:00:00'
                });
            }
        },

        // A list of {starts-at, duration} ISO 8601 local date time and durations that define
        // unavailable time periods for which the studio cannot be booked.
        occupancies: <PropOptions<Object[]>>{ type: Array, default() { return []; } },

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
        },

        // If `true`, tries to display the input elements on a single line on larger screens.
        compact: { type: Boolean, required: false, default: false }
    },
    data() {
        var date = null;
        var time = null;
        var duration = this.value.duration ? this.value.duration : null;

        if (this.value['begins-at']) {
            let mBeginsAt = moment(this.value['begins-at']);
            date = dateComponent(mBeginsAt);
            time = timeComponent(mBeginsAt, 'minutes');
        }

        return {
            date: date,
            time: time,
            duration: duration,
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

        mOccupencies() {
            return this.occupancies.map(o => {
                let beginsAt = moment(o['begins-at']);
                let duration = moment.duration(o['duration']);

                return {
                    beginsAt: beginsAt,
                    endsAt: beginsAt.clone().add(duration),
                };
            });
        },

        // The max (inclusive) date value to be passed to the `<input type="date">` widget.
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

        // When date is set, return the MomentJS date-time at which the studio opens.
        dateOpensAt() {
            if (!this.mDate || this.isClosed) {
                return null;
            }

            let opensAtTime = this.dateSchedule['opens-at'];

            return withTimeComponent(this.mDate, opensAtTime);
        },

        // When date is set, return the MomentJS date-time at which the studio closes.
        dateClosesAt() {
            if (!this.mDate || this.isClosed) {
                return null;
            }

            let opensAtTime = this.dateSchedule['opens-at'];
            let closesAtTime = this.dateSchedule['closes-at'];

            // Close time might be on the next day.
            let closesAtDate = closesAtTime <= opensAtTime
                ? this.mDate.clone().add(1, 'days')
                : this.mDate;

            return withTimeComponent(closesAtDate, closesAtTime);
        },

        isClosed() {
            return this.dateSchedule && !this.dateSchedule['is-open'];
        },

        // Returns the occupency events of the currently select date.
        dateOccupencies() {
            if (!this.mDate) {
                return null;
            }

            return this.mOccupencies.
                filter(o => {
                    // Occupency events overlapping with the currently select date.
                    return eventsOverlap(this.dateOpensAt, this.dateClosesAt, o.beginsAt, o.endsAt);
                }).
                map(o => {
                    // Occupency events should be truncated to the day's opening and closing
                    // times.

                    let beginsAt = moment.max(o.beginsAt, this.dateOpensAt);
                    let endsAt = moment.min(o.endsAt, this.dateClosesAt);
                    return {
                        beginsAt: beginsAt,
                        endsAt: endsAt,
                    };
                });
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

                // Rounds the opening time to the next multiple of `bookingRoundingTime`.

                let rounding_time = NC_CONFIG.bookingBeginsRoundingTime / 60;

                let minutes = iTime.minutes();
                if (minutes % rounding_time) {
                    iTime.add(rounding_time - (minutes % rounding_time), 'minutes');
                }

                let times = [];
                while (iTime.isSameOrBefore(lastSession)) {
                    // Skips start times which will always overlap with an occupency event.
                    let earliestEnd = iTime.clone().add(this.minBookingDuration, 'seconds');
                    // TODO: directly iterate to the next availaible slot.

                    if (!this.dateOccupencyOverlap(iTime, earliestEnd)) {
                        times.push({
                            value: iTime.format('HH:mm'),
                            disabled: false
                        });
                    }

                    iTime.add(rounding_time, 'minutes');
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
            if (!this.beginsAt || this.isClosed) {
                return null;
            }

            let maxDurationMillis = this.dateClosesAt.diff(this.beginsAt);

            let durations = [];
            var iDuration = this.mMinBookingDuration.clone();
            while (iDuration.asMilliseconds() <= maxDurationMillis) {
                // Stops when it starts to overlap with one event of the selected day.
                let endsAt = this.beginsAt.clone().add(iDuration);
                if (this.dateOccupencyOverlap(this.beginsAt, endsAt)) {
                    break;
                }

                durations.push({
                    value: iDuration.asSeconds(),
                    title: renderDuration(iDuration, 'minutes'),
                });
                iDuration.add(NC_CONFIG.bookingDurationRoundingTime, 'seconds');
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
        },

        // Finds the a possible overlap to the provided [beginAt; endsAt[ event from one of the
        // occupencies of the selected day. Returns undefined if no event is found.
        dateOccupencyOverlap(beginsAt, endsAt): boolean {
            if (!this.dateOccupencies) {
                return null;
            }

            return this.dateOccupencies.
                find(o => eventsOverlap(beginsAt, endsAt, o.beginsAt, o.endsAt));
        }
    },
});
</script>

<style>
</style>
