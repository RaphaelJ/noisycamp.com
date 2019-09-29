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

  Provides an interactive calendar.
-->

<template>
    <div>
        <div class="calendar">
            <div class="day-labels">
                <div class="day-label"></div>
                <div
                    class="day-label"
                    v-for="abbr in dayAbbrs"
                    :key="abbr">
                    <div class="day-label-name">{{ abbr }}</div>
                    <small class="day-label-date">Oct 23</small>
                </div>
            </div>

            <div class="schedule">
                <div class="schedule-container">
                    <div class="hour-labels">
                        <div
                            class="hour-label"
                            v-for="index in 24"
                            :key="index">
                            <small>{{ index - 1 }}:00</small>
                        </div>
                    </div>

                    <div
                        class="day"
                        v-for="abbr in dayAbbrs"
                        :key="abbr">

                        <div
                            class="hour"
                            v-for="index in 24"
                            :key="index">
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from "vue";

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        timezone: { type: String, required: true },
    },
    data() {
        return {
            dayAbbrs: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
        }
    },
    methods: {
        hoursInDay(day) {
            return 24;
        }
    },
});
</script>

<style>
.calendar {
    width: 100%;
}

/* Top header */

.calendar .day-labels {
    display: flex;
    flex-direction: row;
}

.calendar .day-labels .day-label {
    flex-grow: 1;
    flex-basis: 0;

    text-align: center;
}

.calendar .day-labels .day-label .day-label-date {
    display: block;
    margin-top: -0.3rem;
}

/* Schedule */

.calendar .schedule {
    position: relative;

    height: 100%;
    overflow-y: scroll;
}

.calendar .schedule:after {
    /* Keeps a 1:2 aspect ratio. */
    padding-top: calc(1 / 2 * 100%);
    display: block;
    content: '';
}

.calendar .schedule .schedule-container {
    position: absolute;
    top: 0;
    bottom: 0;
    right: 0;
    left: 0;

    display: flex;
    flex-direction: row;

    /* Only shows 14h of the day. */
    height: calc(100% / 14 * 24);
}

.calendar .schedule .hour-labels,
.calendar .schedule .day {
    flex-grow: 1;
    flex-basis: 0;

    display: flex;
    flex-direction: column;

    min-height: calc(24 * 25px);
}

.calendar .schedule .hour-labels {
    text-align: center;
}

.calendar .schedule .hour-labels .hour-label,
.calendar .schedule .day .hour {
    flex-grow: 1;
    flex-basis: 0;

    min-height: 25px;

    box-sizing: border-box;
    border-bottom: 1px solid rgba(0, 0, 0, 0.07);
}

.calendar .schedule .day {
    box-sizing: border-box;
    border-left: 1px solid rgba(0, 0, 0, 0.12);
}
</style>
