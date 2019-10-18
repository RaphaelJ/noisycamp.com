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
            <div class="week-nav-buttons clearfix">
                <button class="week-nav-previous" @click="previousWeek()">
                    <arrow direction="left"></arrow>
                </button>
                <button class="week-nav-next" @click="nextWeek()">
                    <arrow direction="right"></arrow>
                </button>

                <div class="week-current-dates">
                    {{ currentWeekDays[0].format('D MMM YYYY') }} -
                    {{ currentWeekDays[6].format('D MMM YYYY') }}
                </div>
            </div>

            <div class="day-labels">
                <div class="day-label"></div>
                <div
                    class="day-label"
                    v-for="day in 7"
                    :key="day">
                    <div class="day-label-name">
                        {{ currentWeekDays[day - 1].format("ddd") }}
                    </div>
                    <small class="day-label-date">
                        {{ currentWeekDays[day - 1].format("MMM D") }}
                    </small>
                </div>
            </div>

            <div class="schedule" ref="schedule">
                <div class="schedule-container">
                    <div class="hour-labels">
                        <div
                            class="hour-label"
                            v-for="hour in 24"
                            :key="hour"
                            :ref="'scheduleHour' + (hour - 1)">
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
                        v-for="(event, index) in currentWeekEvents"
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

import Arrow from '../widgets/Arrow.vue';
import { withTimeComponent } from '../../misc/DateUtils';

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        // The current calendar time, without timezone.
        currentTime: { type: String, required: true },

        // An array of {is-open, opens-at, closes-at} 7 objects. Starts on Monday.
        openingSchedule: <PropOptions<Object[]>>{ type: Array, required: true },

        // The list of events to be displayed in the calendar, with calendar
        // local dates as ISO 8601 strings.
        events: <PropOptions<Object[]>>{ type: Array, required: true },

        // The CSS classes of the event.
        classes: <PropOptions<Object[]>>{ type: Array, default: () => [] },
    },
    data() {
        return {
            // The current position of the calendar, compared to `currentTime`.
            currentWeekOffset: 0
        }
    },
    mounted() {
        // Scrolls the schedule to the 9th hour.
        this.$refs.schedule.scroll(0, this.$refs.scheduleHour9[0].offsetTop);
    },
    computed: {

        // Current server time as a MomentJS object.
        mCurrentTime() {
            return moment(this.currentTime);
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

        // The current week Monday's date at 00:00.
        currentWeek() {
            return this.mCurrentTime.clone()
                .startOf('isoWeek')
                .add(this.currentWeekOffset, 'week');
        },

        // The next week Monday's date at 00:00.
        currentWeekEnd() {
            return this.currentWeek.clone().add(7, 'days');
        },

        // Returns day dates (at 00:00) of the current week.
        currentWeekDays() {
            let dates = Array(7);
            for (var i = 0; i < 7; ++i) {
                dates[i] = this.currentWeek.clone().add(i, 'days');
            }
            return dates;
        },

        // Returns the closed hours as event objects.
        openingScheduleEvents() {
            var prevDay = this.openingSchedule[6];

            let events = [];

            for (var i = 0; i < 7; ++i) {
                // In case of the previous day closing during today's night.
                var prevDayOverlap;
                if (
                    prevDay['is-open']
                    && prevDay['closes-at'] < prevDay['opens-at']
                ) {
                    prevDayOverlap = prevDay['closes-at'];
                } else {
                    prevDayOverlap = '00:00';
                }

                let todaySDate = this.currentWeekDays[i];
                let tomorrowSDate = todaySDate.clone().add(1, 'day');

                let today = this.openingSchedule[i];

                if (today['is-open']) {
                    events.push({ // Morning closure
                        startsAt: withTimeComponent(todaySDate, prevDayOverlap),
                        endsAt: withTimeComponent(
                            todaySDate, today['opens-at']
                        ),
                        classes: ['closing-time']
                    });

                    if (today['closes-at'] > today['opens-at']) {
                        events.push({ // Evening closure
                            startsAt: withTimeComponent(
                                todaySDate, today['closes-at']
                            ),
                            endsAt: withTimeComponent(tomorrowSDate, '00:00'),
                            classes: ['closing-time']
                        });
                    }
                } else {
                    events.push({
                        startsAt: withTimeComponent(todaySDate, prevDayOverlap),
                        endsAt: withTimeComponent(tomorrowSDate, '00:00'),
                        classes: ['closing-time']
                    });
                }

                prevDay = today;
            }

            return events;
        },

        // Events of the currently shown week.
        currentWeekEvents() {
            let monday = this.currentWeek;
            let nextMonday = monday.clone().add(7, 'days');

            let events = this.mEvents.filter(event => {
                    return this.datesOverlap(
                        monday, nextMonday, event.startsAt, event.endsAt
                    );
                })

            return events.concat(this.openingScheduleEvents);
        },
    },
    methods: {
        nextWeek() {
            ++this.currentWeekOffset;
        },

        previousWeek() {
            --this.currentWeekOffset;
        },

        eventClasses(event) {
            return ['event'].concat(event.classes);
        },

        // Returns true if the two date spans overlap.
        //
        // End values are non-inclusive.
        datesOverlap(start1, end1, start2, end2) {
            return (start1.isSameOrBefore(start2) && start2.isBefore(end1))
                || (start2.isSameOrBefore(start1) && start1.isBefore(end2));
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

            // Limits the startsAt and endsAt values to this week's dates.
            let startsAt = moment.max(event.startsAt, this.currentWeek);
            let endsAt = moment.min(event.endsAt, this.currentWeekEnd);

            let startDay = startsAt.clone().startOf('day');
            let endsDay = endsAt.clone().startOf('day');

            // Loops over the days the event is applied on.
            let nDays = endsDay.diff(startDay, 'days') + 1;
            let styles = Array(nDays);
            for (var i = 0; i < nDays; ++i) {
                let day = startDay.clone().add(i, 'days');

                // Does the event starts during the day?
                let startTimeDec =
                    i == 0
                    ? timeDecimal(startsAt)
                    : 0;

                // Does the event stops during the day?
                let endTimeDec =
                    i == nDays - 1
                    ?  timeDecimal(endsAt)
                    : 24;

                styles[i] = {
                    top: `calc(${startTimeDec} / 24 * 100%)`,
                    left: `calc(${day.isoWeekday()} / 8 * 100%)`,
                    height: `calc(${endTimeDec - startTimeDec} / 24 * 100%)`
                };
            }

            return styles;
        }
    },
    components: { Arrow },
});
</script>

<style>
.calendar {
    width: 100%;
}

/* Week navigation buttons */

.calendar .week-nav-buttons .week-current-dates {
    line-height: 2.95rem;
    text-align: center;
}

.calendar .week-nav-buttons .week-nav-previous {
    float: left;
}

.calendar .week-nav-buttons .week-nav-next {
    float: right;
}

.calendar .week-nav-buttons button {
    cursor: pointer;
    opacity: 0.7;

    margin: 0.6rem;
}

.calendar .week-nav-buttons button[disabled] {
    opacity: 1;
    cursor: default;
}

.calendar .week-nav-buttons button:hover {
    opacity: 1;
}

.calendar .week-nav-buttons button svg {
    fill: #6c6c6c;

    height: 1.75rem;
    width: 1.75rem;
}

.calendar .week-nav-buttons button[disabled] svg {
    fill: #dfdcdb;
}

/* Makes the navigation button slighlty larger on small screens. */
@media screen and (max-width: 39.9375em) {
    .calendar .week-nav-buttons .week-current-dates {
        line-height: 3.2rem;
    }

    .calendar .week-nav-buttons button svg {
        height: 2rem;
        width: 2rem;
    }
}

/* Top day labels */

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
    min-height: calc(24 * 25px);
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

.calendar .schedule .event.striped,
.calendar .schedule .event.closing-time {
    background: repeating-linear-gradient(
        -45deg, #f5f3f2c2, #f5f3f2c2 5px, #dfdcdbc2 5px, #dfdcdbc2 10px
    );
}
</style>
