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
                <button
                    :style="{ visibility: hasPreviousWeek ? 'visible' : 'hidden' }"
                    class="week-nav-previous"
                    @click="previousWeek()">
                    <arrow direction="left"></arrow>
                </button>
                <button
                    :style="{ visibility: hasNextWeek ? 'visible' : 'hidden' }"
                    class="week-nav-next" @click="nextWeek()">
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
                    :class="{
                        'today': isToday(currentWeekDays[day - 1]),
                        'out-of-range': !isDayInRange(currentWeekDays[day - 1])
                    }"
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

            <div class="schedule" ref="schedule" :class="{ 'scrollable': scrollable }">
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
                        :class="{
                            'out-of-range': !isDayInRange(currentWeekDays[day - 1]),
                        }"
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
                        <!-- Displays one event <div> for each day the event occurs. -->
                        <div
                            v-for="(style, styleIndex) in eventStyles(event)"
                            :key="'event-' + index + '-style-' + styleIndex"
                            :class="eventClasses(event)"
                            :style="style">
                            <a
                                v-if="event.href"
                                class="event-link-container"
                                :href="event.href">
                                <div v-if="styleIndex == 0 && event.title">
                                    <div class="times">
                                        {{ event.beginsAt.format('HH:mm') }}
                                        <span v-if="event.duration" class="show-for-large-only">
                                            - {{ event.duration.asHours() }} hours
                                        </span>
                                    </div>
                                    <div class="title text-overflow-ellipsis">
                                        {{ event.title }}
                                    </div>
                                </div>
                            </a>
                            <div v-else>
                                <div v-if="styleIndex == 0 && event.title">
                                    <div class="times">
                                        {{ event.beginsAt.format('HH:mm') }}
                                        <span v-if="event.duration" class="show-for-large-only">
                                            - {{ event.duration.asHours() }} hours
                                        </span>
                                    </div>
                                    <div class="title text-overflow-ellipsis">
                                        {{ event.title }}
                                    </div>
                                </div>
                            </div>
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
import { eventsOverlap, withTimeComponent } from '../../misc/DateUtils';

declare var NC_CONFIG: any;

export default Vue.extend({
    props: {
        // The current calendar time (as an  ISO 8601 string), without timezone.
        currentTime: { type: String, required: true },

        // The first (inclusive) and last (exclusive) dates (as ISO 8601 strings) of the calendar.
        begin: { type: String, required: false },
        end: { type: String, required: false },

        // An array of {is-open, opens-at, closes-at} 7 objects. Starts on Monday.
        openingSchedule: <PropOptions<Object[]>>{ type: Array, required: true },

        // The list of events ({title, href, classes, begins-at, ends-at, duration}) to be displayed
        // in the calendar, with calendar local dates as ISO 8601 strings.
        events: <PropOptions<Object[]>>{ type: Array, default() { return []; } },

        // The default CSS classes that will be associated with every event.
        classes: <PropOptions<Object[]>>{ type: Array, default: () => [] },

        // If true, the calendar will only show 14h and be scrollable.
        scrollable: { type: Boolean, default: true },
    },
    data() {
        return {
            currentWeekOffset: 0,
        };
    },
    mounted() {
        if (this.scrollable) {
            // Scrolls the schedule to the 9th hour.
            this.$refs.schedule.scroll(0, this.$refs.scheduleHour9[0].offsetTop);
        }
    },
    computed: {
        // Current times and dates as a MomentJS object.

        mCurrentTime() {
            return moment(this.currentTime);
        },

        mBegin() {
            if (this.begin) {
                return moment(this.begin);
            } else {
                return null;
            }
        },

        mEnd() {
            if (this.end) {
                return moment(this.end);
            } else {
                return null;
            }
        },

        // Calendar events with MomentJS objects.
        mEvents() {
            return this.events
                .map(event => {
                    let beginsAt = moment(event['begins-at']);

                    var endsAt, duration;

                    // Either `duration` or `ends-at` should be provided.
                    if ('ends-at' in event) {
                        endsAt = moment(event['ends-at']);
                        duration =  moment.duration(endsAt.diff(beginsAt));
                    } else {
                        duration = moment.duration(event['duration']);
                        endsAt = beginsAt.clone().add(duration);
                    }

                    return {
                        beginsAt: beginsAt,
                        endsAt: endsAt,
                        duration: duration,
                        classes: event['classes'],
                        title: event['title'],
                        href: event['href'],
                    };
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

        hasPreviousWeek() {
            return !this.begin || this.mBegin.isBefore(this.currentWeek, 'day');
        },

        hasNextWeek() {
            return !this.end || this.mEnd.isSameOrAfter(this.currentWeekEnd, 'day');
        },

        // Returns the closed hours as event objects.
        openingScheduleEvents() {
            var prevDay = this.openingSchedule[6];

            let events = [];

            for (var i = 0; i < 7; ++i) {
                let todayDate = this.currentWeekDays[i];

                if (!this.isDayInRange(todayDate)) { // Skips not in rage day.
                    continue;
                }

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

                let tomorrowDate = todayDate.clone().add(1, 'day');

                let today = this.openingSchedule[i];

                if (today['is-open']) {
                    events.push({ // Morning closure
                        beginsAt: withTimeComponent(todayDate, prevDayOverlap),
                        endsAt: withTimeComponent(
                            todayDate, today['opens-at']
                        ),
                    });

                    if (today['closes-at'] > today['opens-at']) {
                        events.push({ // Evening closure
                            beginsAt: withTimeComponent(
                                todayDate, today['closes-at']
                            ),
                            endsAt: withTimeComponent(tomorrowDate, '00:00'),
                        });
                    }
                } else {
                    events.push({
                        beginsAt: withTimeComponent(todayDate, prevDayOverlap),
                        endsAt: withTimeComponent(tomorrowDate, '00:00'),
                    });
                }

                prevDay = today;
            }

            return events.map(event =>
                Object.assign(event, { classes: ["closed"], isOpeningScheduleEvent: true })
            );
        },

        // Events of the currently shown week.
        currentWeekEvents() {
            let monday = this.currentWeek;
            let nextMonday = this.currentWeekEnd;

            let events = this.mEvents.filter(event => {
                    return eventsOverlap(
                        monday, nextMonday, event.beginsAt, event.endsAt
                    );
                })

            let currEvents = events.concat(this.openingScheduleEvents);

            return currEvents;
        },
    },
    methods: {
        nextWeek() {
            if (this.hasNextWeek) {
                ++this.currentWeekOffset;
            }
        },

        previousWeek() {
            if (this.hasPreviousWeek) {
                --this.currentWeekOffset;
            }
        },

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

            // Limits the beginsAt and endsAt values to this week's dates.
            let beginsAt = moment.max(event.beginsAt, this.currentWeek);
            let endsAt = moment.min(event.endsAt, this.currentWeekEnd);

            let beginDay = beginsAt.clone().startOf('day');
            let endsDay = endsAt.clone().startOf('day');

            // Loops over the days the event is applied on.
            let nDays = endsDay.diff(beginDay, 'days') + 1;
            let styles = Array(nDays);
            for (var i = 0; i < nDays; ++i) {
                let day = beginDay.clone().add(i, 'days');

                // Does the event starts during the day?
                let beginTimeDec =
                    i == 0
                    ? timeDecimal(beginsAt)
                    : 0;

                // Does the event stops during the day?
                let endTimeDec =
                    i == nDays - 1
                    ?  timeDecimal(endsAt)
                    : 24;

                styles[i] = {
                    top: `calc(${beginTimeDec} / 24 * 100%)`,
                    left: `calc(${day.isoWeekday()} / 8 * 100%)`,
                    height: `calc(${endTimeDec - beginTimeDec} / 24 * 100%)`
                };
            }

            return styles;
        },

        // Returns true if the day of the given date is within the calendar [begin..end( range.
        isDayInRange(date) {
            return (!this.begin || this.mBegin.isSameOrBefore(date, 'day'))
                && (!this.end || this.mEnd.isAfter(date, 'day'));
        },

        isToday(date) {
            return this.mCurrentTime.isSame(date, 'day');
        },
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

.calendar .day-labels .day-label.today {
    font-weight: bold;
}

.calendar .day-labels .day-label.out-of-range {
    opacity: 0.3;
}

/* Schedule */

.calendar .schedule {
    position: relative;
}

.calendar .schedule .schedule-container {
    display: flex;
    flex-direction: row;
}

.calendar .schedule.scrollable {
    height: 100%;
    overflow-y: scroll;
}

.calendar .schedule.scrollable:after {
    /* Keeps a 1:2 aspect ratio. */
    padding-top: calc(1 / 2 * 100%);
    display: block;
    content: '';
}

.calendar .schedule.scrollable .schedule-container {
    position: absolute;
    top: 0;
    bottom: 0;
    right: 0;
    left: 0;

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
}

.calendar .schedule .day:not(.out-of-range),
.calendar .schedule .day.out-of-range:first-of-type {
    border-left: 1px solid rgba(0, 0, 0, 0.12);
}

.calendar .schedule .day.out-of-range .hour {
    display: none;
}

/* Events */

.calendar .schedule .event {
    position: absolute;

    width: calc(100% / 8);

    overflow-y: auto;
}

.calendar .schedule .event .event-link-container {
    display: block;
    width: 100%;
    height: 100%;

    color: unset;
}

.calendar .schedule .event.striped,
.calendar .schedule .event.closed,
.calendar .schedule .event.occupied {
    background: repeating-linear-gradient(
        -45deg, #f5f3f2c2, #f5f3f2c2 5px, #dfdcdbc2 5px, #dfdcdbc2 10px
    );
}

.calendar .schedule .event.booking {
    padding: 0.2rem;

    border: 1px solid #b37216;
    border-left: 4px solid #b37216;
    background-color: #b372167a;
}

.calendar .schedule .event.booking .times {
    font-size: 0.75rem;
}

.calendar .schedule .event.booking .title {
    font-size: 0.85rem;
}
</style>
