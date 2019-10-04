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
                    v-for="day in 7"
                    :key="day">
                    <div class="day-label-name">
                        {{ weekDays[day - 1].format("ddd") }}
                    </div>
                    <small class="day-label-date">
                        {{ weekDays[day - 1].format("MMM D") }}
                    </small>
                </div>
            </div>

            <div class="schedule">
                <div class="schedule-container">
                    <div class="hour-labels">
                        <div
                            class="hour-label"
                            v-for="hour in 24"
                            :key="hour">
                            <small>{{ hour - 1 }}:00</small>
                        </div>
                    </div>

                    <div
                        class="day"
                        v-for="day in 7"
                        :key="day">

                        <div
                            class="hour"
                            v-for="hour in 24"
                            :key="hour">
                        </div>
                    </div>

                    <div
                        v-for="(event, index) in weekEvents"
                        :key="'event-' + index"
                        class="event-container">
                        <div
                            v-for="(style, styleIndex) in eventStyles(event)"
                            :key="'event-' + index + '-style-' + styleIndex"
                            :class="eventClasses(event)"
                            :style="style">
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import Vue, { PropOptions } from "vue";

import * as moment from 'moment';

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        // The current calendar time, without timezone.
        currentTime: { type: String, required: true },

        // The list of events to be displayed in the calendar, with calendar
        // local dates as ISO 8601 strings.
        events: <PropOptions<Object[]>>{ type: Array, required: true },

        // The CSS classes of the event.
        classes: <PropOptions<Object[]>>{ type: Array, default: () => [] },
    },
    data() {
        return { }
    },
    computed: {
        // Current time as a MomentJS object.
        mCurrentTime() {
            return moment(this.currentTime)
        },

        // Calendar events with MomentJS objects.
        mEvents() {
            return this.events
                .map(event => {
                    return {
                        startsAt: moment(event['starts-at']),
                        endsAt: moment(event['ends-at']),
                        classes: event['classes']
                    }
                });
        },

        // Returns the date (at 00:00) of the current week's Monday.
        weekStart() {
            return this.mCurrentTime.startOf('isoWeek');
        },

        // Returns day dates (at 00:00) of the current week.
        weekDays() {
            let dates = Array(7);
            for (var i = 0; i < 7; ++i) {
                dates[i] = this.weekStart.clone().add(i, 'days');
            }
            return dates;
        },

        // Events of the currently shown week.
        weekEvents() {
            let monday = this.weekStart;
            let nextMonday = monday.clone().add(7, 'days');

            return this.mEvents
                .filter(event => {
                    return event.startsAt >= monday && event.startsAt < nextMonday;
                });
        },
    },
    methods: {
        eventClasses(event) {
            return ['event'].concat(event.classes);
        },

        // Returns a list of CSS positioning parameters for event (one for each day the event
        // applies).
        eventStyles(event) {
            // Converts the time part of a date+time value to a decimal value in
            // [0..24].
            function timeDecimal(datetime) {
                return datetime.hour()
                    + datetime.minute() / 60.0
                    + datetime.second() / 3600.0;
            }

            let startDay = event.startsAt.clone().startOf('day');
            let endsDay = event.endsAt.clone().startOf('day');

            // Loops over the days the event is applied on.
            let nDays = endsDay.diff(startDay, 'days') + 1;
            let styles = Array(nDays);
            for (var i = 0; i < nDays; ++i) {
                let day = startDay.clone().add(i, 'days');

                // Does the event starts during the day?
                let startTimeDec =
                    i == 0
                    ? timeDecimal(event.startsAt)
                    : 0;

                // Does the event stops during the day?
                let endTimeDec =
                    i == nDays - 1
                    ?  timeDecimal(event.endsAt)
                    : 24;

                console.log(day);
                console.log(startTimeDec);
                console.log(endTimeDec);
                console.log();

                styles[i] = {
                    top: `calc(${startTimeDec} / 24 * 100%)`,
                    left: `calc(${day.isoWeekday()} / 8 * 100%)`,
                    height: `calc(${endTimeDec - startTimeDec} / 24 * 100%)`
                };
            }

            return styles;
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

/* Events */

.calendar .schedule .event {
    position: absolute;

    width: calc(100% / 8);
}

.calendar .schedule .event.striped {
    background: repeating-linear-gradient(
        -45deg, #f5f3f2c2, #f5f3f2c2 5px, #dfdcdbc2 5px, #dfdcdbc2 10px
    );
}
</style>
